package com.prof18.feedflow.android.widget

internal const val MIN_WIDGET_FONT_SCALE = -4
internal const val MAX_WIDGET_FONT_SCALE = 12

private const val WIDGET_HEADER_FONT_SIZE = 18
private const val WIDGET_TITLE_FONT_SIZE = 14
private const val WIDGET_META_FONT_SIZE = 12
private const val WIDGET_EMPTY_FONT_SIZE = 16

internal data class WidgetFontSizes(
    val header: Int,
    val title: Int,
    val meta: Int,
    val empty: Int,
)

internal fun widgetFontSizes(scaleFactor: Int): WidgetFontSizes {
    val clampedScale = scaleFactor.coerceIn(MIN_WIDGET_FONT_SCALE, MAX_WIDGET_FONT_SCALE)
    return WidgetFontSizes(
        header = WIDGET_HEADER_FONT_SIZE + clampedScale,
        title = WIDGET_TITLE_FONT_SIZE + clampedScale,
        meta = WIDGET_META_FONT_SIZE + clampedScale,
        empty = WIDGET_EMPTY_FONT_SIZE + clampedScale,
    )
}
