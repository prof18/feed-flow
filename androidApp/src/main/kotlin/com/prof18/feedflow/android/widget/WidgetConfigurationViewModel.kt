package com.prof18.feedflow.android.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val settingsRepository: SettingsRepository,
    private val feedDownloadWorkerEnqueuer: FeedDownloadWorkerEnqueuer,
) : ViewModel() {

    private val _syncPeriodState = MutableStateFlow<SyncPeriod>(SyncPeriod.ONE_HOUR)
    val syncPeriodState: StateFlow<SyncPeriod> = _syncPeriodState.asStateFlow()

    private val _feedLayoutState = MutableStateFlow<FeedLayout>(FeedLayout.LIST)
    val feedLayoutState: StateFlow<FeedLayout> = _feedLayoutState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentPeriod = settingsRepository.getSyncPeriod()
            val currentFeedLayout = settingsRepository.getFeedLayout()
            _syncPeriodState.update {
                if (currentPeriod == SyncPeriod.NEVER) {
                    SyncPeriod.ONE_HOUR
                } else {
                    currentPeriod
                }
            }
            _feedLayoutState.update { currentFeedLayout }
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        _syncPeriodState.update { period }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        _feedLayoutState.update { feedLayout }
    }

    fun enqueueWorker() {
        settingsRepository.setSyncPeriod(syncPeriodState.value)
        settingsRepository.setFeedLayout(feedLayoutState.value)
        feedDownloadWorkerEnqueuer.updateWorker(syncPeriodState.value)
    }
}
