package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.intui.standalone.styling.light
import org.jetbrains.jewel.ui.component.styling.LocalSliderStyle
import org.jetbrains.jewel.ui.component.styling.SliderColors
import org.jetbrains.jewel.ui.component.styling.SliderMetrics
import org.jetbrains.jewel.ui.component.styling.SliderStyle

// Stepped sliders keep snapping, keyboard steps and semantics; the value label next to each slider
// shows the exact value, so the dense step markers under the track are hidden.
@Composable
internal fun ProvideSliderStyles(content: @Composable () -> Unit) {
    val style = LocalSliderStyle.current
    CompositionLocalProvider(
        LocalSliderStyle provides remember(style) { style.withoutStepMarkers() },
        content = content,
    )
}

internal fun SliderStyle.withoutStepMarkers(): SliderStyle = SliderStyle(
    // The light/dark factories only differ in their defaults; every color is passed explicitly here.
    colors = SliderColors.light(
        track = colors.track,
        trackFilled = colors.trackFilled,
        trackDisabled = colors.trackDisabled,
        trackFilledDisabled = colors.trackFilledDisabled,
        stepMarker = Color.Transparent,
        thumbFill = colors.thumbFill,
        thumbFillDisabled = colors.thumbFillDisabled,
        thumbFillFocused = colors.thumbFillFocused,
        thumbFillPressed = colors.thumbFillPressed,
        thumbFillHovered = colors.thumbFillHovered,
        thumbBorder = colors.thumbBorder,
        thumbBorderFocused = colors.thumbBorderFocused,
        thumbBorderDisabled = colors.thumbBorderDisabled,
        thumbBorderPressed = colors.thumbBorderPressed,
        thumbBorderHovered = colors.thumbBorderHovered,
    ),
    metrics = SliderMetrics(
        trackHeight = metrics.trackHeight,
        thumbSize = metrics.thumbSize,
        thumbBorderWidth = metrics.thumbBorderWidth,
        stepLineHeight = 0.dp,
        stepLineWidth = 0.dp,
        trackToStepSpacing = 0.dp,
    ),
    thumbShape = thumbShape,
)
