package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.AutoDeletePeriod
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.model.SyncAndStorageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncAndStorageSettingsViewModel internal constructor(
    private val settingsRepository: SettingsRepository,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : ViewModel() {

    private val stateMutableFlow = MutableStateFlow(SyncAndStorageState())
    val state: StateFlow<SyncAndStorageState> = stateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.syncPeriodFlow.collect { syncPeriod ->
                loadSettings(syncPeriod)
            }
        }
    }

    private fun loadSettings(syncPeriod: SyncPeriod) {
        val autoDeletePeriod = settingsRepository.getAutoDeletePeriod()
        val refreshFeedsOnLaunch = settingsRepository.getRefreshFeedsOnLaunch()

        stateMutableFlow.update {
            SyncAndStorageState(
                syncPeriod = syncPeriod,
                autoDeletePeriod = autoDeletePeriod,
                refreshFeedsOnLaunch = refreshFeedsOnLaunch,
            )
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        viewModelScope.launch {
            settingsRepository.setSyncPeriod(period)
            stateMutableFlow.update {
                it.copy(syncPeriod = period)
            }
        }
    }

    fun updateAutoDeletePeriod(period: AutoDeletePeriod) {
        viewModelScope.launch {
            settingsRepository.setAutoDeletePeriod(period)
            stateMutableFlow.update {
                it.copy(autoDeletePeriod = period)
            }
        }
    }

    fun updateRefreshFeedsOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRefreshFeedsOnLaunch(enabled)
            stateMutableFlow.update {
                it.copy(refreshFeedsOnLaunch = enabled)
            }
        }
    }

    fun clearDownloadedArticleContent() {
        viewModelScope.launch {
            feedItemContentFileHandler.clearAllContent()
        }
    }
}
