package com.prof18.feedflow.shared.data

import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetCardAppearance
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.domain.model.normalized
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WidgetSettingsRepository(
    private val settings: Settings,
) {
    private val widgetCardAppearanceLock = Any()

    private val feedWidgetLayoutMutableFlow = MutableStateFlow(getFeedWidgetLayout())
    val feedWidgetLayout: StateFlow<WidgetFeedLayout> = feedWidgetLayoutMutableFlow.asStateFlow()

    private val widgetShowHeaderMutableFlow = MutableStateFlow(getWidgetShowHeader())
    val widgetShowHeader: StateFlow<Boolean> = widgetShowHeaderMutableFlow.asStateFlow()

    private val widgetFontScaleMutableFlow = MutableStateFlow(getWidgetFontScaleFactor())
    val widgetFontScale: StateFlow<Int> = widgetFontScaleMutableFlow.asStateFlow()

    private val widgetBackgroundColorMutableFlow = MutableStateFlow(getWidgetBackgroundColor())
    val widgetBackgroundColor: StateFlow<Int?> = widgetBackgroundColorMutableFlow.asStateFlow()

    private val widgetBackgroundOpacityMutableFlow = MutableStateFlow(getWidgetBackgroundOpacityPercent())
    val widgetBackgroundOpacity: StateFlow<Int> = widgetBackgroundOpacityMutableFlow.asStateFlow()

    private val widgetTextColorModeMutableFlow = MutableStateFlow(getWidgetTextColorMode())
    val widgetTextColorMode: StateFlow<WidgetTextColorMode> = widgetTextColorModeMutableFlow.asStateFlow()

    private val widgetHideImagesMutableFlow = MutableStateFlow(getWidgetHideImages())
    val widgetHideImages: StateFlow<Boolean> = widgetHideImagesMutableFlow.asStateFlow()

    private val widgetCardAppearanceMutableFlow = MutableStateFlow(getWidgetCardAppearance())
    val widgetCardAppearance: StateFlow<WidgetCardAppearance> = widgetCardAppearanceMutableFlow.asStateFlow()

    fun getFeedWidgetLayout(): WidgetFeedLayout =
        settings.getString(WidgetSettingsFields.FEED_WIDGET_LAYOUT.name, WidgetFeedLayout.LIST.name)
            .let { storedLayout ->
                runCatching { WidgetFeedLayout.valueOf(storedLayout) }
                    .getOrDefault(WidgetFeedLayout.LIST)
            }

    fun setFeedWidgetLayout(feedLayout: WidgetFeedLayout) {
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

    fun getWidgetTextColorMode(): WidgetTextColorMode =
        settings.getString(
            WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE.name,
            WidgetTextColorMode.AUTOMATIC.name,
        ).let { WidgetTextColorMode.valueOf(it) }

    fun setWidgetTextColorMode(value: WidgetTextColorMode) {
        settings[WidgetSettingsFields.WIDGET_TEXT_COLOR_MODE.name] = value.name
        widgetTextColorModeMutableFlow.update { value }
    }

    fun getWidgetHideImages(): Boolean =
        settings.getBoolean(WidgetSettingsFields.WIDGET_HIDE_IMAGES.name, false)

    fun setWidgetHideImages(value: Boolean) {
        settings[WidgetSettingsFields.WIDGET_HIDE_IMAGES.name] = value
        widgetHideImagesMutableFlow.update { value }
    }

    fun getWidgetCardAppearance(): WidgetCardAppearance = synchronized(widgetCardAppearanceLock) {
        val defaults = WidgetCardAppearance()
        WidgetCardAppearance(
            surfaceColor = settings.getIntOrNull(
                WidgetSettingsFields.WIDGET_CARD_SURFACE_COLOR.name,
            ),
            surfaceOpacityPercent = settings.getInt(
                WidgetSettingsFields.WIDGET_CARD_SURFACE_OPACITY_PERCENT.name,
                defaults.surfaceOpacityPercent,
            ),
            cornerRadiusDp = settings.getInt(
                WidgetSettingsFields.WIDGET_CARD_CORNER_RADIUS_DP.name,
                defaults.cornerRadiusDp,
            ),
            itemSeparation = getEnumOrDefault(
                field = WidgetSettingsFields.WIDGET_CARD_ITEM_SEPARATION,
                default = defaults.itemSeparation,
            ),
            dividerOpacityPercent = settings.getInt(
                WidgetSettingsFields.WIDGET_CARD_DIVIDER_OPACITY_PERCENT.name,
                defaults.dividerOpacityPercent,
            ),
            imageSizing = getEnumOrDefault(
                field = WidgetSettingsFields.WIDGET_CARD_IMAGE_SIZING,
                default = defaults.imageSizing,
            ),
        ).normalized()
    }

    fun setWidgetCardAppearance(value: WidgetCardAppearance) {
        synchronized(widgetCardAppearanceLock) {
            val normalizedValue = value.normalized()
            if (normalizedValue.surfaceColor == null) {
                settings.remove(WidgetSettingsFields.WIDGET_CARD_SURFACE_COLOR.name)
            } else {
                settings[WidgetSettingsFields.WIDGET_CARD_SURFACE_COLOR.name] = normalizedValue.surfaceColor
            }
            settings[WidgetSettingsFields.WIDGET_CARD_SURFACE_OPACITY_PERCENT.name] =
                normalizedValue.surfaceOpacityPercent
            settings[WidgetSettingsFields.WIDGET_CARD_CORNER_RADIUS_DP.name] = normalizedValue.cornerRadiusDp
            settings[WidgetSettingsFields.WIDGET_CARD_ITEM_SEPARATION.name] = normalizedValue.itemSeparation.name
            settings[WidgetSettingsFields.WIDGET_CARD_DIVIDER_OPACITY_PERCENT.name] =
                normalizedValue.dividerOpacityPercent
            settings[WidgetSettingsFields.WIDGET_CARD_IMAGE_SIZING.name] = normalizedValue.imageSizing.name
            widgetCardAppearanceMutableFlow.update { normalizedValue }
        }
    }

    private inline fun <reified T : Enum<T>> getEnumOrDefault(
        field: WidgetSettingsFields,
        default: T,
    ): T {
        val storedValue = settings.getString(field.name, default.name)
        return enumValues<T>().firstOrNull { it.name == storedValue } ?: default
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
    WIDGET_TEXT_COLOR_MODE,
    WIDGET_HIDE_IMAGES,
    WIDGET_CARD_SURFACE_COLOR,
    WIDGET_CARD_SURFACE_OPACITY_PERCENT,
    WIDGET_CARD_CORNER_RADIUS_DP,
    WIDGET_CARD_ITEM_SEPARATION,
    WIDGET_CARD_DIVIDER_OPACITY_PERCENT,
    WIDGET_CARD_IMAGE_SIZING,
}
