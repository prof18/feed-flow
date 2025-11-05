package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.nextcloud.NextcloudCredentials
import com.prof18.feedflow.feedsync.nextcloud.NextcloudDataSource
import com.prof18.feedflow.feedsync.nextcloud.NextcloudSettings
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NextcloudSyncViewModel internal constructor(
    private val logger: Logger,
    private val nextcloudSettings: NextcloudSettings,
    private val nextcloudDataSource: NextcloudDataSource,
    private val feedSyncRepository: FeedSyncRepository,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : ViewModel() {

    private val nextcloudSyncUiMutableState = MutableStateFlow<AccountConnectionUiState>(
        AccountConnectionUiState.Unlinked,
    )
    val nextcloudConnectionUiState: StateFlow<AccountConnectionUiState> = nextcloudSyncUiMutableState.asStateFlow()

    val syncMessageQueue = feedSyncMessageQueue.messageQueue

    init {
        restoreNextcloudAuth()
    }

    fun saveCredentials(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            try {
                nextcloudSyncUiMutableState.update { AccountConnectionUiState.Loading }

                val credentials = NextcloudCredentials(serverUrl, username, password)
                nextcloudDataSource.setup(credentials)

                val isConnected = nextcloudDataSource.testConnection()
                if (isConnected) {
                    nextcloudSyncUiMutableState.update {
                        AccountConnectionUiState.Linked(
                            syncState = getSyncState(),
                        )
                    }
                    emitSyncLoading()
                    accountsRepository.setNextcloudAccount()
                    feedSyncRepository.firstSync()
                    feedFetcherRepository.fetchFeeds()
                    emitLastSyncUpdate()
                } else {
                    logger.e { "Nextcloud connection test failed" }
                    nextcloudSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
                }
            } catch (e: Throwable) {
                logger.e(e) { "Error while trying to connect to Nextcloud" }
                nextcloudSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
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
                nextcloudSyncUiMutableState.update { AccountConnectionUiState.Loading }
                nextcloudDataSource.revokeAccess()
                feedSyncRepository.deleteAll()
                accountsRepository.clearAccount()
                nextcloudSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
            } catch (e: Exception) {
                logger.e(e) { "Error while unlinking Nextcloud" }
            }
        }
    }

    private fun restoreNextcloudAuth() {
        nextcloudSyncUiMutableState.update { AccountConnectionUiState.Loading }
        if (nextcloudSettings.hasCredentials()) {
            nextcloudSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
        } else {
            nextcloudSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            nextcloudSettings.getLastDownloadTimestamp() != null || nextcloudSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun getLastUploadDate(): String? =
        nextcloudSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        nextcloudSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun emitSyncLoading() {
        nextcloudSyncUiMutableState.update { oldState ->
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
        nextcloudSyncUiMutableState.update { oldState ->
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
