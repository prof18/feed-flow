package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.model.DropboxSynMessages
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxException
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.getDxCredentialsAsString
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncMessageQueue
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DropboxSyncViewModel internal constructor(
    private val logger: Logger,
    private val dropboxSettings: DropboxSettings,
    private val dropboxDataSource: DropboxDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val accountsRepository: AccountsRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : ViewModel() {

    private val dropboxSyncUiMutableState = MutableStateFlow<AccountConnectionUiState>(
        AccountConnectionUiState.Unlinked,
    )
    val dropboxConnectionUiState: StateFlow<AccountConnectionUiState> = dropboxSyncUiMutableState.asStateFlow()

    private val dropboxSyncMessageMutableState = MutableSharedFlow<DropboxSynMessages>()
    val dropboxSyncMessageState: SharedFlow<DropboxSynMessages> = dropboxSyncMessageMutableState.asSharedFlow()

    val syncMessageQueue = feedSyncMessageQueue.messageQueue

    init {
        restoreDropboxAuth()
    }

    fun saveDropboxAuth() {
        viewModelScope.launch {
            try {
                val stringCredentials = getDxCredentialsAsString()
                dropboxDataSource.saveAuth(DropboxStringCredentials(stringCredentials))
                dropboxSettings.setDropboxData(stringCredentials)
                dropboxSyncUiMutableState.update {
                    AccountConnectionUiState.Linked(
                        syncState = getSyncState(),
                    )
                }
                emitSyncLoading()
                accountsRepository.setDropboxAccount()
                feedSyncRepository.firstSync()
                feedRetrieverRepository.fetchFeeds()
                emitLastSyncUpdate()
            } catch (e: Throwable) {
                logger.e(e) { "Error while trying to auth with Dropbox after getting the code" }
                dropboxSyncMessageMutableState.emit(DropboxSynMessages.Error)
                dropboxSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
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
                dropboxSyncUiMutableState.update { AccountConnectionUiState.Loading }
                dropboxDataSource.revokeAccess()
                dropboxSettings.clearDropboxData()
                feedSyncRepository.deleteAll()
                accountsRepository.clearAccount()
                dropboxSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            } catch (_: DropboxException) {
                dropboxSyncMessageMutableState.emit(DropboxSynMessages.Error)
            }
        }
    }

    private fun restoreDropboxAuth() {
        dropboxSyncUiMutableState.update { AccountConnectionUiState.Loading }
        val stringCredentials = dropboxSettings.getDropboxData()
        if (stringCredentials != null) {
            dropboxDataSource.restoreAuth(DropboxStringCredentials(stringCredentials))
            dropboxSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            dropboxSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            dropboxSettings.getLastDownloadTimestamp() != null || dropboxSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun getLastUploadDate(): String? =
        dropboxSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        dropboxSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        dropboxSyncUiMutableState.update { oldState ->
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
        dropboxSyncUiMutableState.update { oldState ->
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
