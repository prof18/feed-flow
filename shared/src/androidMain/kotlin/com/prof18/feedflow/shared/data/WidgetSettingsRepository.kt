package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.FeedLayout
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WidgetSettingsRepository(
    private val settings: Settings,
) {
    private val feedWidgetLayoutMutableFlow = MutableStateFlow(getFeedWidgetLayout())
    val feedWidgetLayout: StateFlow<FeedLayout> = feedWidgetLayoutMutableFlow.asStateFlow()

    private val widgetShowHeaderMutableFlow = MutableStateFlow(getWidgetShowHeader())
    val widgetShowHeader: StateFlow<Boolean> = widgetShowHeaderMutableFlow.asStateFlow()

    private val widgetFontScaleMutableFlow = MutableStateFlow(getWidgetFontScaleFactor())
    val widgetFontScale: StateFlow<Int> = widgetFontScaleMutableFlow.asStateFlow()

    private val widgetBackgroundColorMutableFlow = MutableStateFlow(getWidgetBackgroundColor())
    val widgetBackgroundColor: StateFlow<Int?> = widgetBackgroundColorMutableFlow.asStateFlow()

    private val widgetBackgroundOpacityMutableFlow = MutableStateFlow(getWidgetBackgroundOpacityPercent())
    val widgetBackgroundOpacity: StateFlow<Int> = widgetBackgroundOpacityMutableFlow.asStateFlow()

    fun getFeedWidgetLayout(): FeedLayout =
        settings.getString(WidgetSettingsFields.FEED_WIDGET_LAYOUT.name, FeedLayout.LIST.name)
            .let { FeedLayout.valueOf(it) }

    fun setFeedWidgetLayout(feedLayout: FeedLayout) {
        settings[WidgetSettingsFields.FEED_WIDGET_LAYOUT.name] = feedLayout.name
        feedWidgetLayoutMutableFlow.update { feedLayout }
    }

    fun getWidgetShowHeader(): Boolean =
        settings.getBoolean(WidgetSettingsFields.WIDGET_SHOW_HEADER.name, true)

    fun setWidgetShowHeader(value: Boolean) {
        settings[WidgetSettingsFields.WIDGET_SHOW_HEADER.name] = value
        widgetShowHeaderMutableFlow.update { value }
    }

    fun getWidgetFontScaleFactor(): Int =
        settings.getInt(
            WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR.name,
            DEFAULT_WIDGET_FONT_SCALE_FACTOR,
        )

    fun setWidgetFontScaleFactor(value: Int) {
        settings[WidgetSettingsFields.WIDGET_FONT_SCALE_FACTOR.name] = value
        widgetFontScaleMutableFlow.update { value }
    }

    fun getWidgetBackgroundColor(): Int? =
        settings.getIntOrNull(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR.name)

    fun setWidgetBackgroundColor(colorArgb: Int?) {
        if (colorArgb == null) {
            settings.remove(WidgetSettingsFields.WIDGET_BACKGROUND_COLOR.name)
        } else {
            settings[WidgetSettingsFields.WIDGET_BACKGROUND_COLOR.name] = colorArgb
        }
        widgetBackgroundColorMutableFlow.update { colorArgb }
    }

    fun getWidgetBackgroundOpacityPercent(): Int =
        settings.getInt(
            WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT.name,
            DEFAULT_WIDGET_BACKGROUND_OPACITY_PERCENT,
        )

    fun setWidgetBackgroundOpacityPercent(value: Int) {
        settings[WidgetSettingsFields.WIDGET_BACKGROUND_OPACITY_PERCENT.name] = value
        widgetBackgroundOpacityMutableFlow.update { value }
    }

    private companion object {
        const val DEFAULT_WIDGET_FONT_SCALE_FACTOR = 0
        const val DEFAULT_WIDGET_BACKGROUND_OPACITY_PERCENT = 100
    }
}

private enum class WidgetSettingsFields {
    FEED_WIDGET_LAYOUT,
    WIDGET_SHOW_HEADER,
    WIDGET_FONT_SCALE_FACTOR,
    WIDGET_BACKGROUND_COLOR,
    WIDGET_BACKGROUND_OPACITY_PERCENT,
}
