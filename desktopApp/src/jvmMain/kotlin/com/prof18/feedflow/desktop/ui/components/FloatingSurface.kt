package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme

/**
 * Opaque, raised surface for controls that float over scrolling content. In dark themes a shadow alone
 * barely reads, so the surface is lifted slightly above the panel color and gets a hairline border.
 */
@Composable
internal fun Modifier.floatingSurface(): Modifier {
    val shape = RoundedCornerShape(DesktopDesignTokens.buttonCornerSize)
    val panel = JewelTheme.globalColors.panelBackground
    val isDark = JewelTheme.isDark
    val surface = if (isDark) JewelTheme.globalColors.text.normal.copy(alpha = 0.08f).compositeOver(panel) else panel
    val base = shadow(elevation = 6.dp, shape = shape).background(surface, shape)
    return if (isDark) {
        base.border(width = 1.dp, color = JewelTheme.globalColors.borders.normal.copy(alpha = 0.6f), shape = shape)
    } else {
        base
    }
}
