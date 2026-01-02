package com.prof18.feedflow.android.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.android.widget.WidgetSettingsState
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

    private val _settingsState = MutableStateFlow(
        WidgetSettingsState(),
    )
    val settingsState: StateFlow<WidgetSettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentPeriod = settingsRepository.getSyncPeriod()
            val currentFeedLayout = settingsRepository.getFeedWidgetLayout()
            val currentShowHeader = settingsRepository.getWidgetShowHeader()
            val currentFontScale = settingsRepository.getWidgetFontScaleFactor()
            val currentBackgroundColor = settingsRepository.getWidgetBackgroundColor()
            val currentBackgroundOpacity = settingsRepository.getWidgetBackgroundOpacityPercent()
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
        if (_settingsState.value.syncPeriod == period) {
            return
        }
        _settingsState.update { it.copy(syncPeriod = period) }
        settingsRepository.setSyncPeriod(period)
        feedDownloadWorkerEnqueuer.updateWorker(period)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        if (_settingsState.value.feedLayout == feedLayout) {
            return
        }
        _settingsState.update { it.copy(feedLayout = feedLayout) }
        settingsRepository.setFeedWidgetLayout(feedLayout)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateShowHeader(showHeader: Boolean) {
        if (_settingsState.value.showHeader == showHeader) {
            return
        }
        _settingsState.update { it.copy(showHeader = showHeader) }
        settingsRepository.setWidgetShowHeader(showHeader)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateFontScale(scaleFactor: Int) {
        if (_settingsState.value.fontScale == scaleFactor) {
            return
        }
        _settingsState.update { it.copy(fontScale = scaleFactor) }
        settingsRepository.setWidgetFontScaleFactor(scaleFactor)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateBackgroundColor(colorArgb: Int?) {
        if (_settingsState.value.backgroundColor == colorArgb) {
            return
        }
        _settingsState.update { it.copy(backgroundColor = colorArgb) }
        settingsRepository.setWidgetBackgroundColor(colorArgb)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateBackgroundOpacityPercent(opacityPercent: Int) {
        if (_settingsState.value.backgroundOpacityPercent == opacityPercent) {
            return
        }
        _settingsState.update { it.copy(backgroundOpacityPercent = opacityPercent) }
        settingsRepository.setWidgetBackgroundOpacityPercent(opacityPercent)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }
}
