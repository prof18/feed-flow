package com.prof18.feedflow.android.e2e

import android.content.SharedPreferences
import com.prof18.feedflow.core.model.WidgetFeedLayout
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode

internal object E2eWidgetSettingsSeedHelper {

    fun resetLegacyFallbacks(settings: SharedPreferences) {
        settings.edit()
            .putString(FEED_WIDGET_LAYOUT, WidgetFeedLayout.LIST.name)
            .putBoolean(WIDGET_SHOW_HEADER, true)
            .putInt(WIDGET_FONT_SCALE_FACTOR, 0)
            .remove(WIDGET_BACKGROUND_COLOR)
            .putInt(WIDGET_BACKGROUND_OPACITY_PERCENT, DEFAULT_BACKGROUND_OPACITY_PERCENT)
            .putString(WIDGET_TEXT_COLOR_MODE, WidgetTextColorMode.AUTOMATIC.name)
            .putBoolean(WIDGET_HIDE_IMAGES, false)
            .apply()
    }

    fun applyAndroidWidgetLegacyFallbacks(settings: SharedPreferences) {
        settings.edit()
            .putString(FEED_WIDGET_LAYOUT, WidgetFeedLayout.CARD.name)
            .putBoolean(WIDGET_SHOW_HEADER, true)
            .putInt(WIDGET_FONT_SCALE_FACTOR, 2)
            .putInt(WIDGET_BACKGROUND_COLOR, ANDROID_WIDGET_BACKGROUND_COLOR)
            .putInt(WIDGET_BACKGROUND_OPACITY_PERCENT, ANDROID_WIDGET_BACKGROUND_OPACITY_PERCENT)
            .putString(WIDGET_TEXT_COLOR_MODE, WidgetTextColorMode.LIGHT.name)
            .putBoolean(WIDGET_HIDE_IMAGES, false)
            .apply()
    }

    private const val FEED_WIDGET_LAYOUT = "FEED_WIDGET_LAYOUT"
    private const val WIDGET_SHOW_HEADER = "WIDGET_SHOW_HEADER"
    private const val WIDGET_FONT_SCALE_FACTOR = "WIDGET_FONT_SCALE_FACTOR"
    private const val WIDGET_BACKGROUND_COLOR = "WIDGET_BACKGROUND_COLOR"
    private const val WIDGET_BACKGROUND_OPACITY_PERCENT = "WIDGET_BACKGROUND_OPACITY_PERCENT"
    private const val WIDGET_TEXT_COLOR_MODE = "WIDGET_TEXT_COLOR_MODE"
    private const val WIDGET_HIDE_IMAGES = "WIDGET_HIDE_IMAGES"
    private const val DEFAULT_BACKGROUND_OPACITY_PERCENT = 100
    private const val ANDROID_WIDGET_BACKGROUND_OPACITY_PERCENT = 85
    private const val ANDROID_WIDGET_BACKGROUND_COLOR = 0xFF1E3A5F.toInt()
}
