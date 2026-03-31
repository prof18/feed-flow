package com.prof18.feedflow.android.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode

private const val LIGHT_BACKGROUND_LUMINANCE_THRESHOLD = 0.179f
private const val SECONDARY_TEXT_ALPHA = 0.72f

internal data class WidgetTextColors(
    val primary: Color,
    val secondary: Color,
)

internal fun widgetTextColorsForMode(
    textColorMode: WidgetTextColorMode,
    backgroundColor: Color,
): WidgetTextColors {
    return when (textColorMode) {
        WidgetTextColorMode.AUTOMATIC -> widgetTextColorsForBackground(backgroundColor)
        WidgetTextColorMode.LIGHT -> WidgetTextColors(
            primary = Color.White,
            secondary = Color.White.copy(alpha = SECONDARY_TEXT_ALPHA),
        )
        WidgetTextColorMode.DARK -> WidgetTextColors(
            primary = Color.Black,
            secondary = Color.Black.copy(alpha = SECONDARY_TEXT_ALPHA),
        )
    }
}

internal fun widgetEffectiveBackgroundColor(
    backgroundColor: Color,
    backgroundOpacity: Float,
    underlayColor: Color,
): Color {
    return backgroundColor
        .copy(alpha = backgroundOpacity)
        .compositeOver(underlayColor)
}

internal fun widgetTextColorsForBackground(backgroundColor: Color): WidgetTextColors {
    val opaqueBackgroundColor = backgroundColor.copy(alpha = 1f)
    val primaryTextColor = if (opaqueBackgroundColor.luminance() > LIGHT_BACKGROUND_LUMINANCE_THRESHOLD) {
        Color.Black
    } else {
        Color.White
    }

    return WidgetTextColors(
        primary = primaryTextColor,
        secondary = primaryTextColor.copy(alpha = SECONDARY_TEXT_ALPHA),
    )
}
