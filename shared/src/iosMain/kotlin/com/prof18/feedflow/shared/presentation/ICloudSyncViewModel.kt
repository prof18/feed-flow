package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AccountConnectionUiState
import com.prof18.feedflow.core.model.AccountSyncUIState
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.feedsync.AccountsRepository
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncMessageQueue
import com.prof18.feedflow.shared.domain.feedsync.FeedSyncRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ICloudSyncViewModel internal constructor(
    private val iCloudSettings: ICloudSettings,
    private val dateFormatter: DateFormatter,
    private val accountsRepository: AccountsRepository,
    private val feedSyncRepository: FeedSyncRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
    feedSyncMessageQueue: FeedSyncMessageQueue,
) : ViewModel() {

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
        viewModelScope.launch {
            iCloudSettings.setUseICloud(true)
            accountsRepository.setICloudAccount()
            iCloudSyncUiMutableState.update {
                AccountConnectionUiState.Linked(
                    syncState = getSyncState(),
                )
            }
            emitSyncLoading()
            accountsRepository.setICloudAccount()
            feedSyncRepository.firstSync()
            feedRetrieverRepository.fetchFeeds()
            emitLastSyncUpdate()
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
            iCloudSyncUiMutableState.update { AccountConnectionUiState.Loading }
            iCloudSettings.setUseICloud(false)
            feedSyncRepository.deleteAll()
            accountsRepository.clearAccount()
            iCloudSyncUiMutableState.update { AccountConnectionUiState.Unlinked }
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

    private fun emitSyncLoading() {
        iCloudSyncUiMutableState.update { oldState ->
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
        iCloudSyncUiMutableState.update { oldState ->
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
