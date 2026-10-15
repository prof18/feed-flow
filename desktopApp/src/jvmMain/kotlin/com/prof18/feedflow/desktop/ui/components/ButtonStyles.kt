package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.styling.ButtonMetrics
import org.jetbrains.jewel.ui.component.styling.ButtonStyle
import org.jetbrains.jewel.ui.theme.outlinedButtonStyle

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
