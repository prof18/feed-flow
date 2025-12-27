package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GoogleDriveSyncViewModel internal constructor(
    private val googleDriveSettings: GoogleDriveSettings,
    private val googleDriveDataSource: GoogleDriveDataSourceJvm,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
) : ViewModel() {

    private val googleDriveSyncUiMutableState =
        MutableStateFlow<AccountConnectionUiState>(AccountConnectionUiState.Unlinked)
    val googleDriveConnectionUiState: StateFlow<AccountConnectionUiState> = googleDriveSyncUiMutableState.asStateFlow()

    private val gDriveSyncMessageMutableState = MutableSharedFlow<GoogleDriveSynMessages>()
    val googleDriveSyncMessageState: SharedFlow<GoogleDriveSynMessages> = gDriveSyncMessageMutableState.asSharedFlow()

    init {
        restoreGoogleDriveAuth()
    }

    fun startGoogleDriveAuthFlow() {
        viewModelScope.launch {
            googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
            val success = googleDriveDataSource.startAuthFlow()
            if (success) {
                accountsRepository.setGoogleDriveAccount()
                googleDriveSyncUiMutableState.update {
                    AccountConnectionUiState.Linked(syncState = getSyncState())
                }
                emitSyncLoading()
                feedSyncRepository.firstSync()
                feedFetcherRepository.fetchFeeds()
                emitLastSyncUpdate()
            } else {
                gDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            }
        }
    }

    private fun restoreGoogleDriveAuth() {
        googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
        val isRestored = googleDriveDataSource.restoreAuth()
        if (isRestored) {
            googleDriveSyncUiMutableState.update {
                AccountConnectionUiState.Linked(syncState = getSyncState())
            }
        } else {
            googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            emitSyncLoading()
            feedSyncRepository.performBackup(forceBackup = true)
            emitLastSyncUpdate()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
                googleDriveDataSource.revokeAccess()
                feedSyncRepository.deleteAll()
                accountsRepository.clearAccount()
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            } catch (_: GoogleDriveException) {
                gDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
            }
        }
    }

    private fun getLastUploadDate(): String? =
        googleDriveSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        googleDriveSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        googleDriveSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = AccountSyncUIState.Loading,
                )
            } else {
                oldState
            }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            googleDriveSettings.getLastDownloadTimestamp() != null ||
                googleDriveSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun emitLastSyncUpdate() {
        googleDriveSyncUiMutableState.update { oldState ->
            if (oldState is AccountConnectionUiState.Linked) {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            } else {
                oldState
            }
        }
    }
}
