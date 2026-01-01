package com.prof18.feedflow.android.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val feedDownloadWorkerEnqueuer: FeedDownloadWorkerEnqueuer,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val _syncPeriodState = MutableStateFlow(settingsRepository.getSyncPeriod())
    val syncPeriodState: StateFlow<SyncPeriod> = _syncPeriodState.asStateFlow()

    private val _feedLayoutState = MutableStateFlow(settingsRepository.getFeedWidgetLayout())
    val feedLayoutState: StateFlow<FeedLayout> = _feedLayoutState.asStateFlow()

    fun updateSyncPeriod(period: SyncPeriod) {
        if (_syncPeriodState.value == period) {
            return
        }
        _syncPeriodState.update { period }
        settingsRepository.setSyncPeriod(period)
        feedDownloadWorkerEnqueuer.updateWorker(period)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        if (_feedLayoutState.value == feedLayout) {
            return
        }
        _feedLayoutState.update { feedLayout }
        settingsRepository.setFeedWidgetLayout(feedLayout)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }
}
