package com.prof18.feedflow.android.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
                settingsRepository.syncPeriodFlow,
                widgetAppearanceSettingsFlow,
                widgetSettingsRepository.widgetHideImages,
            ) { syncPeriod, appearance, hideImages ->
                WidgetSettingsState(
                    syncPeriod = syncPeriod,
                    feedLayout = appearance.feedLayout,
                    showHeader = appearance.showHeader,
                    fontScale = appearance.fontScale,
                    backgroundColor = appearance.backgroundColor,
                    backgroundOpacityPercent = appearance.backgroundOpacity,
                    hideImages = hideImages,
                )
            }.collect { state ->
                _settingsState.update { state }
            }
        }
    }

    fun updateFeedLayout(feedLayout: FeedLayout) {
        if (_settingsState.value.feedLayout == feedLayout) return
        widgetSettingsRepository.setFeedWidgetLayout(feedLayout)
    }

    fun updateShowHeader(showHeader: Boolean) {
        if (_settingsState.value.showHeader == showHeader) return
        widgetSettingsRepository.setWidgetShowHeader(showHeader)
    }

    fun updateFontScale(scaleFactor: Int) {
        if (_settingsState.value.fontScale == scaleFactor) return
        widgetSettingsRepository.setWidgetFontScaleFactor(scaleFactor)
    }

    fun updateBackgroundColor(colorArgb: Int?) {
        if (_settingsState.value.backgroundColor == colorArgb) return
        widgetSettingsRepository.setWidgetBackgroundColor(colorArgb)
    }

    fun updateBackgroundOpacityPercent(opacityPercent: Int) {
        if (_settingsState.value.backgroundOpacityPercent == opacityPercent) return
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(opacityPercent)
    }

    fun updateHideImages(hideImages: Boolean) {
        if (_settingsState.value.hideImages == hideImages) return
        widgetSettingsRepository.setWidgetHideImages(hideImages)
    }

    fun enqueueWorker() {
        // Settings are already persisted by the update methods
    }

    private data class WidgetAppearanceSettings(
        val feedLayout: FeedLayout,
        val showHeader: Boolean,
        val fontScale: Int,
        val backgroundColor: Int?,
        val backgroundOpacity: Int,
    )
}
