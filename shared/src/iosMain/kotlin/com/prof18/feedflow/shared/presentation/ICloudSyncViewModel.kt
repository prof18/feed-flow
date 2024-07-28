package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncMessageQueue
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ICloudSyncViewModel internal constructor(
    private val iCloudSettings: ICloudSettings,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : BaseViewModel() {

    private val iCloudSyncUiMutableState = MutableStateFlow<AccountConnectionUiState>(
        AccountConnectionUiState.Unlinked,
    )

    @NativeCoroutinesState
    val iCloudConnectionUiState: StateFlow<AccountConnectionUiState> = iCloudSyncUiMutableState.asStateFlow()

    @NativeCoroutines
    val syncMessageQueue = feedSyncMessageQueue.messageQueue

    init {
        restoreICloudAuth()
    }

    fun setICloudAuth() {
        iCloudSettings.setUseICloud(true)
        accountsRepository.setICloudAccount()
        iCloudSyncUiMutableState.update {
            AccountConnectionUiState.Linked(
                syncState = getSyncState(),
            )
        }
        /*
        emitSyncLoading()
                accountsRepository.setDropboxAccount()
                feedSyncRepository.firstSync()
                feedRetrieverRepository.fetchFeeds()
                emitLastSyncUpdate()
         */
    }

    fun triggerBackup() {
        // TODO
    }

    fun unlink() {
        iCloudSettings.setUseICloud(false)
//        feedSyncRepository.deleteAll()
        accountsRepository.clearAccount()
        iCloudSyncUiMutableState.update {
            AccountConnectionUiState.Unlinked
        }
    }

    private fun restoreICloudAuth() {
        val useIcloud = iCloudSettings.getUseICloud()
        iCloudSyncUiMutableState.update {
            if (useIcloud) {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            } else {
                AccountConnectionUiState.Unlinked
            }
        }
    }

    private fun getSyncState(): AccountSyncUIState {
        return when {
            iCloudSettings.getLastDownloadTimestamp() != null || iCloudSettings.getLastUploadTimestamp() != null -> {
                AccountSyncUIState.Synced(
                    lastDownloadDate = getLastDownloadDate(),
                    lastUploadDate = getLastUploadDate(),
                )
            }

            else -> AccountSyncUIState.None
        }
    }

    private fun getLastUploadDate(): String? =
        iCloudSettings.getLastUploadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }

    private fun getLastDownloadDate(): String? =
        iCloudSettings.getLastDownloadTimestamp()?.let { timestamp ->
            dateFormatter.formatDateForLastRefresh(timestamp)
        }
}
