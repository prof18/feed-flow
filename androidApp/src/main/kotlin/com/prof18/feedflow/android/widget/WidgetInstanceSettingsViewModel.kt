package com.prof18.feedflow.android.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.WidgetContentFilter
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.data.WidgetConfiguration
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.domain.model.SyncPeriod
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.presentation.WidgetUpdater
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WidgetInstanceSettingsViewModel(
    private val appWidgetId: Int,
    settingsRepository: SettingsRepository,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    feedWidgetRepository: FeedWidgetRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    private val _settingsState = MutableStateFlow(
        widgetSettingsRepository.getWidgetConfiguration(appWidgetId).toSettingsState(),
    )
    val settingsState: StateFlow<WidgetSettingsState> = _settingsState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.syncPeriodFlow,
                feedWidgetRepository.observeFeedSourceCategories(),
                feedWidgetRepository.observeFeedSources(),
            ) { syncPeriod, categories, feedSources ->
                WidgetPickerState(
                    syncPeriod = syncPeriod,
                    categories = categories.toImmutableList(),
                    feedSources = feedSources.toImmutableList(),
                )
            }.collect { pickerState ->
                _settingsState.update { currentState ->
                    currentState.copy(
                        syncPeriod = pickerState.syncPeriod,
                        categories = pickerState.categories,
                        feedSources = pickerState.feedSources,
                    )
                }
            }
        }
    }

    fun updateContentFilter(contentFilter: WidgetContentFilter) {
        if (_settingsState.value.contentFilter == contentFilter) {
            return
        }
        _settingsState.update { it.copy(contentFilter = contentFilter) }
        widgetSettingsRepository.setWidgetContentFilter(appWidgetId, contentFilter)
        updateWidgets()
    }

    fun reloadWidgetConfiguration() {
        val configuration = widgetSettingsRepository.getWidgetConfiguration(appWidgetId)
        _settingsState.update { currentState ->
            configuration.toSettingsState().copy(
                syncPeriod = currentState.syncPeriod,
                categories = currentState.categories,
                feedSources = currentState.feedSources,
            )
        }
    }

    fun updateFeedLayout(feedLayout: WidgetFeedLayout) {
        if (_settingsState.value.feedLayout == feedLayout) {
            return
        }
        _settingsState.update { it.copy(feedLayout = feedLayout) }
        widgetSettingsRepository.setWidgetFeedLayout(appWidgetId, feedLayout)
        updateWidgets()
    }

    fun updateShowHeader(showHeader: Boolean) {
        if (_settingsState.value.showHeader == showHeader) {
            return
        }
        _settingsState.update { it.copy(showHeader = showHeader) }
        widgetSettingsRepository.setWidgetShowHeader(appWidgetId, showHeader)
        updateWidgets()
    }

    fun updateShowRefreshButton(showRefreshButton: Boolean) {
        if (_settingsState.value.showRefreshButton == showRefreshButton) {
            return
        }
        _settingsState.update { it.copy(showRefreshButton = showRefreshButton) }
        widgetSettingsRepository.setWidgetShowRefreshButton(appWidgetId, showRefreshButton)
        updateWidgets()
    }

    fun updateFontScale(scaleFactor: Int) {
        if (_settingsState.value.fontScale == scaleFactor) {
            return
        }
        _settingsState.update { it.copy(fontScale = scaleFactor) }
        widgetSettingsRepository.setWidgetFontScaleFactor(appWidgetId, scaleFactor)
        updateWidgets()
    }

    fun updateBackgroundColor(colorArgb: Int?) {
        if (_settingsState.value.backgroundColor == colorArgb) {
            return
        }
        _settingsState.update { it.copy(backgroundColor = colorArgb) }
        widgetSettingsRepository.setWidgetBackgroundColor(appWidgetId, colorArgb)
        updateWidgets()
    }

    fun updateBackgroundOpacityPercent(opacityPercent: Int) {
        if (_settingsState.value.backgroundOpacityPercent == opacityPercent) {
            return
        }
        _settingsState.update { it.copy(backgroundOpacityPercent = opacityPercent) }
        widgetSettingsRepository.setWidgetBackgroundOpacityPercent(appWidgetId, opacityPercent)
        updateWidgets()
    }

    fun updateTextColorMode(textColorMode: WidgetTextColorMode) {
        if (_settingsState.value.textColorMode == textColorMode) {
            return
        }
        _settingsState.update { it.copy(textColorMode = textColorMode) }
        widgetSettingsRepository.setWidgetTextColorMode(appWidgetId, textColorMode)
        updateWidgets()
    }

    fun updateHideImages(hideImages: Boolean) {
        if (_settingsState.value.hideImages == hideImages) {
            return
        }
        _settingsState.update { it.copy(hideImages = hideImages) }
        widgetSettingsRepository.setWidgetHideImages(appWidgetId, hideImages)
        updateWidgets()
    }

    private fun updateWidgets() {
        viewModelScope.launch {
            widgetUpdater.update()
        }
    }
}

private fun WidgetConfiguration.toSettingsState() = WidgetSettingsState(
    feedLayout = feedLayout,
    showHeader = showHeader,
    showRefreshButton = showRefreshButton,
    fontScale = fontScale,
    backgroundColor = backgroundColor,
    backgroundOpacityPercent = backgroundOpacityPercent,
    textColorMode = textColorMode,
    hideImages = hideImages,
    contentFilter = contentFilter,
)

private data class WidgetPickerState(
    val syncPeriod: SyncPeriod,
    val categories: ImmutableList<FeedSourceCategory>,
    val feedSources: ImmutableList<FeedSource>,
)
