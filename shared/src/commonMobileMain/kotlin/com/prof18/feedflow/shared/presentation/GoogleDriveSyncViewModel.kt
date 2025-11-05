package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.GoogleDriveSynMessages
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveCredentials
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSource
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveStringCredentials
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
    private val logger: Logger,
    private val googleDriveSettings: GoogleDriveSettings,
    private val googleDriveDataSource: GoogleDriveDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : ViewModel() {

    private val googleDriveSyncUiMutableState = MutableStateFlow<AccountConnectionUiState>(
        AccountConnectionUiState.Unlinked,
    )
    val googleDriveConnectionUiState: StateFlow<AccountConnectionUiState> = googleDriveSyncUiMutableState.asStateFlow()

    private val googleDriveSyncMessageMutableState = MutableSharedFlow<GoogleDriveSynMessages>()
    val googleDriveSyncMessageState: SharedFlow<GoogleDriveSynMessages> = googleDriveSyncMessageMutableState.asSharedFlow()

    val syncMessageQueue = feedSyncMessageQueue.messageQueue

    init {
        restoreGoogleDriveAuth()
    }

    fun saveGoogleDriveAuth(accessToken: String) {
        viewModelScope.launch {
            try {
                val credentials = GoogleDriveCredentials(
                    accessToken = accessToken,
                    refreshToken = null,
                    expiresAtMillis = System.currentTimeMillis() + (3600 * 1000),
                )

                googleDriveSettings.setGoogleDriveCredentials(credentials)
                googleDriveDataSource.saveAuth(GoogleDriveStringCredentials(accessToken))
                googleDriveSyncUiMutableState.update {
                    AccountConnectionUiState.Linked(
                        syncState = getSyncState(),
                    )
                }
                emitSyncLoading()
                accountsRepository.setGoogleDriveAccount()
                feedSyncRepository.firstSync()
                feedFetcherRepository.fetchFeeds()
                emitLastSyncUpdate()
            } catch (e: Throwable) {
                logger.e(e) { "Error while trying to auth with Google Drive after getting the code" }
                googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            }
        }
    }

    fun updateAccessToken(accessToken: String) {
        viewModelScope.launch {
            try {
                val credentials = GoogleDriveCredentials(
                    accessToken = accessToken,
                    refreshToken = null,
                    expiresAtMillis = System.currentTimeMillis() + (3600 * 1000),
                )

                googleDriveSettings.setGoogleDriveCredentials(credentials)
                googleDriveDataSource.saveAuth(GoogleDriveStringCredentials(accessToken))
            } catch (e: Throwable) {
                logger.e(e) { "Error while updating Google Drive access token" }
            }
        }
    }

    fun triggerBackup() {
        viewModelScope.launch {
            emitSyncLoading()
            feedSyncRepository.performBackup(forceBackup = true)
            emitLastSyncUpdate()
        }
    }

    fun unlink() {
        viewModelScope.launch {
            try {
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
                googleDriveDataSource.revokeAccess()
                googleDriveSettings.clearGoogleDriveData()
                feedSyncRepository.deleteAll()
                accountsRepository.clearAccount()
                googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            } catch (_: GoogleDriveException) {
                googleDriveSyncMessageMutableState.emit(GoogleDriveSynMessages.Error)
            }
        }
    }

    private fun restoreGoogleDriveAuth() {
        googleDriveSyncUiMutableState.update { AccountConnectionUiState.Loading }
        val credentials = googleDriveSettings.getGoogleDriveCredentials()
        if (credentials != null) {
            googleDriveDataSource.restoreAuth(GoogleDriveStringCredentials(credentials.accessToken))
            googleDriveSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            googleDriveSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            googleDriveSettings.getLastDownloadTimestamp() != null || googleDriveSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
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
