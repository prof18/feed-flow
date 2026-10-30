package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.ButtonMetrics
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.component.styling.IconButtonMetrics
import org.jetbrains.jewel.ui.component.styling.IconButtonStyle
import org.jetbrains.jewel.ui.component.styling.LocalDefaultButtonStyle
import org.jetbrains.jewel.ui.component.styling.LocalIconButtonStyle
import org.jetbrains.jewel.ui.component.styling.LocalOutlinedButtonStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle

@Composable
internal fun ProvideButtonStyles(content: @Composable () -> Unit) {
    val iconStyle = LocalIconButtonStyle.current
    val defaultStyle = LocalDefaultButtonStyle.current
    val outlinedStyle = LocalOutlinedButtonStyle.current
    CompositionLocalProvider(
        LocalIconButtonStyle provides remember(iconStyle) { iconStyle.withRoundedCorners() },
        LocalDefaultButtonStyle provides remember(defaultStyle) { defaultStyle.withRoundedCorners() },
        LocalOutlinedButtonStyle provides remember(outlinedStyle) { outlinedStyle.withRoundedCorners() },
        content = content,
    )
}

private fun IconButtonStyle.withRoundedCorners(): IconButtonStyle = IconButtonStyle(
    colors = colors,
    metrics = IconButtonMetrics(
        cornerSize = CornerSize(DesktopDesignTokens.buttonCornerSize),
        borderWidth = metrics.borderWidth,
        padding = metrics.padding,
        minSize = metrics.minSize,
    ),
)

private fun ButtonStyle.withRoundedCorners(): ButtonStyle = ButtonStyle(
    colors = colors,
    metrics = ButtonMetrics(
        cornerSize = CornerSize(DesktopDesignTokens.buttonCornerSize),
        padding = metrics.padding,
        minSize = metrics.minSize,
        borderWidth = metrics.borderWidth,
        focusOutlineExpand = metrics.focusOutlineExpand,
    ),
    focusOutlineAlignment = focusOutlineAlignment,
)

@Composable
internal fun rememberWrappingOutlinedButtonStyle(): ButtonStyle {
    val style = JewelTheme.outlinedButtonStyle
    // Jewel fixes the inner row to its metric height; wrapped labels need their intrinsic height.
    return remember(style) {
        val metrics = style.metrics
        ButtonStyle(
            colors = style.colors,
            metrics = ButtonMetrics(
                cornerSize = metrics.cornerSize,
                padding = metrics.padding,
                minSize = metrics.minSize.copy(height = Dp.Unspecified),
                borderWidth = metrics.borderWidth,
                focusOutlineExpand = metrics.focusOutlineExpand,
            ),
            focusOutlineAlignment = style.focusOutlineAlignment,
        )
    }
}
