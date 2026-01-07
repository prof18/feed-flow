package com.prof18.feedflow.android.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.FeedDownloadWorkerEnqueuer
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val settingsRepository: SettingsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val feedDownloadWorkerEnqueuer: FeedDownloadWorkerEnqueuer,
) : ViewModel() {

    private val _settingsState = MutableStateFlow(WidgetSettingsState())
    val settingsState: StateFlow<WidgetSettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentPeriod = settingsRepository.getSyncPeriod()
            val currentFeedLayout = widgetSettingsRepository.getFeedWidgetLayout()
            val currentShowHeader = widgetSettingsRepository.getWidgetShowHeader()
            val currentFontScale = widgetSettingsRepository.getWidgetFontScaleFactor()
            val currentBackgroundColor = widgetSettingsRepository.getWidgetBackgroundColor()
            val currentBackgroundOpacity = widgetSettingsRepository.getWidgetBackgroundOpacityPercent()

            _settingsState.update {
                it.copy(
                    syncPeriod = if (currentPeriod == SyncPeriod.NEVER) {
                        SyncPeriod.ONE_HOUR
                    } else {
                        currentPeriod
                    },
                    feedLayout = currentFeedLayout,
                    showHeader = currentShowHeader,
                    fontScale = currentFontScale,
                    backgroundColor = currentBackgroundColor,
                    backgroundOpacityPercent = currentBackgroundOpacity,
                )
            }
        }
    }

    fun updateSyncPeriod(period: SyncPeriod) {
        _settingsState.update { it.copy(syncPeriod = period) }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        _settingsState.update { it.copy(feedLayout = feedLayout) }
    }

    fun updateShowHeader(showHeader: Boolean) {
        _settingsState.update { it.copy(showHeader = showHeader) }
    }

    fun updateFontScale(scaleFactor: Int) {
        _settingsState.update { it.copy(fontScale = scaleFactor) }
    }

    fun updateBackgroundColor(colorArgb: Int?) {
        _settingsState.update { it.copy(backgroundColor = colorArgb) }
    }

    fun updateBackgroundOpacityPercent(opacityPercent: Int) {
        _settingsState.update { it.copy(backgroundOpacityPercent = opacityPercent) }
    }

    fun enqueueWorker() {
        val state = settingsState.value
        settingsRepository.setSyncPeriod(state.syncPeriod)
        widgetSettingsRepository.setFeedWidgetLayout(state.feedLayout)
        widgetSettingsRepository.setWidgetShowHeader(state.showHeader)
        widgetSettingsRepository.setWidgetFontScaleFactor(state.fontScale)
        widgetSettingsRepository.setWidgetBackgroundColor(state.backgroundColor)
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(state.backgroundOpacityPercent)
        feedDownloadWorkerEnqueuer.updateWorker(state.syncPeriod)
    }
}
