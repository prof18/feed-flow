package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

private const val RGB_HEX_LENGTH = 6
private const val RGB_MASK = 0x00FFFFFF
private const val OPAQUE_RGB_PREFIX = 0xFF000000L

internal fun widgetColorFromArgb(colorArgb: Int): Color = Color(colorArgb).copy(alpha = 1f)

internal fun widgetColorToOpaqueArgb(color: Color): Int = color.copy(alpha = 1f).toArgb()

internal fun formatWidgetColorHex(colorArgb: Int): String {
    return String.format(Locale.US, "#%06X", colorArgb and RGB_MASK)
}

internal fun parseWidgetColorHex(input: String): Color? {
    val normalized = input.trim().removePrefix("#")
    if (
        normalized.length != RGB_HEX_LENGTH ||
        normalized.any { !it.isDigit() && it.uppercaseChar() !in 'A'..'F' }
    ) {
        return null
    }

    val rgb = normalized.toLongOrNull(radix = 16) ?: return null
    return Color((OPAQUE_RGB_PREFIX or rgb).toInt())
}
