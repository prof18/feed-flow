package com.prof18.feedflow.android.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetConfigurationViewModel(
    private val settingsRepository: SettingsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
) : ViewModel() {

    private val _settingsState = MutableStateFlow(WidgetSettingsState())
    val settingsState: StateFlow<WidgetSettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentFeedLayout = widgetSettingsRepository.getFeedWidgetLayout()
            val currentShowHeader = widgetSettingsRepository.getWidgetShowHeader()
            val currentFontScale = widgetSettingsRepository.getWidgetFontScaleFactor()
            val currentBackgroundColor = widgetSettingsRepository.getWidgetBackgroundColor()
            val currentBackgroundOpacity = widgetSettingsRepository.getWidgetBackgroundOpacityPercent()

            _settingsState.update {
                it.copy(
                    feedLayout = currentFeedLayout,
                    showHeader = currentShowHeader,
                    fontScale = currentFontScale,
                    backgroundColor = currentBackgroundColor,
                    backgroundOpacityPercent = currentBackgroundOpacity,
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.syncPeriodFlow.collect { syncPeriod ->
                _settingsState.update {
                    it.copy(
                        syncPeriod = syncPeriod,
                    )
                }
            }
        }
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
        widgetSettingsRepository.setFeedWidgetLayout(state.feedLayout)
        widgetSettingsRepository.setWidgetShowHeader(state.showHeader)
        widgetSettingsRepository.setWidgetFontScaleFactor(state.fontScale)
        widgetSettingsRepository.setWidgetBackgroundColor(state.backgroundColor)
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(state.backgroundOpacityPercent)
    }
}
