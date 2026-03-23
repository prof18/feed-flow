package com.prof18.feedflow.android.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.android.widget.WidgetSettingsState
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetSettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val _settingsState = MutableStateFlow(
        WidgetSettingsState(),
    )
    val settingsState: StateFlow<WidgetSettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            val globalSyncSettingsFlow = settingsRepository.syncPeriodFlow

            val widgetAppearanceSettingsFlow = combine(
                widgetSettingsRepository.feedWidgetLayout,
                widgetSettingsRepository.widgetShowHeader,
                widgetSettingsRepository.widgetFontScale,
                widgetSettingsRepository.widgetBackgroundColor,
                widgetSettingsRepository.widgetBackgroundOpacity,
            ) { feedLayout, showHeader, fontScale, backgroundColor, backgroundOpacity ->
                WidgetAppearanceSettings(
                    feedLayout = feedLayout,
                    showHeader = showHeader,
                    fontScale = fontScale,
                    backgroundColor = backgroundColor,
                    backgroundOpacity = backgroundOpacity,
                )
            }

            combine(
                globalSyncSettingsFlow,
                widgetAppearanceSettingsFlow,
            ) { syncPeriod, widgetAppearanceSettings ->
                WidgetSettingsState(
                    syncPeriod = syncPeriod,
                    feedLayout = widgetAppearanceSettings.feedLayout,
                    showHeader = widgetAppearanceSettings.showHeader,
                    fontScale = widgetAppearanceSettings.fontScale,
                    backgroundColor = widgetAppearanceSettings.backgroundColor,
                    backgroundOpacityPercent = widgetAppearanceSettings.backgroundOpacity,
                )
            }.collect { widgetSettingsState ->
                _settingsState.update {
                    widgetSettingsState
                }
            }
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        if (_settingsState.value.feedLayout == feedLayout) {
            return
        }
        _settingsState.update { it.copy(feedLayout = feedLayout) }
        widgetSettingsRepository.setFeedWidgetLayout(feedLayout)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateShowHeader(showHeader: Boolean) {
        if (_settingsState.value.showHeader == showHeader) {
            return
        }
        _settingsState.update { it.copy(showHeader = showHeader) }
        widgetSettingsRepository.setWidgetShowHeader(showHeader)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateFontScale(scaleFactor: Int) {
        if (_settingsState.value.fontScale == scaleFactor) {
            return
        }
        _settingsState.update { it.copy(fontScale = scaleFactor) }
        widgetSettingsRepository.setWidgetFontScaleFactor(scaleFactor)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateBackgroundColor(colorArgb: Int?) {
        if (_settingsState.value.backgroundColor == colorArgb) {
            return
        }
        _settingsState.update { it.copy(backgroundColor = colorArgb) }
        widgetSettingsRepository.setWidgetBackgroundColor(colorArgb)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    fun updateBackgroundOpacityPercent(opacityPercent: Int) {
        if (_settingsState.value.backgroundOpacityPercent == opacityPercent) {
            return
        }
        _settingsState.update { it.copy(backgroundOpacityPercent = opacityPercent) }
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(opacityPercent)
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }

    private data class WidgetAppearanceSettings(
        val feedLayout: FeedLayout,
        val showHeader: Boolean,
        val fontScale: Int,
        val backgroundColor: Int?,
        val backgroundOpacity: Int,
    )
}
