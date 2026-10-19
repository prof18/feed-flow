package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text

@Immutable
data class DrawerItemVisualStyle(
    val itemShape: Shape = CircleShape,
    val itemMinHeight: Dp = 56.dp,
    val itemVerticalPadding: Dp = 0.dp,
    val selectedContainerColor: Color? = null,
    val selectedContentColor: Color? = null,
)

val DefaultDrawerItemVisualStyle = DrawerItemVisualStyle()

@Composable
internal fun desktopDrawerItemVisualStyle(): DrawerItemVisualStyle = DrawerItemVisualStyle(
    itemShape = RoundedCornerShape(DesktopDesignTokens.selectionCornerSize),
    itemMinHeight = DesktopDesignTokens.navigationRowMinHeight,
    itemVerticalPadding = 0.dp,
    selectedContainerColor = sidebarSelectedColor(),
    selectedContentColor = JewelTheme.globalColors.text.normal,
)

/** Small, muted label for sidebar sections such as "Feed Sources", like macOS sidebar headings. */
@Composable
internal fun SidebarSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = DesktopDesignTokens.metadataText.copy(fontWeight = FontWeight.SemiBold),
        color = JewelTheme.globalColors.text.info,
    )
}

// Sidebar rows show state with solid fills only, never outlines. Colors are composited over the
// sidebar background so they stay opaque and consistent in light, dark and OLED themes.
@Composable
@ReadOnlyComposable
internal fun sidebarSelectedColor(): Color = sidebarTint(
    JewelTheme.globalColors.outlines.focused,
    alpha = if (JewelTheme.isDark) 0.34f else 0.18f,
)

@Composable
@ReadOnlyComposable
internal fun sidebarMultiSelectedColor(): Color = sidebarTint(
    JewelTheme.globalColors.outlines.focused,
    alpha = if (JewelTheme.isDark) 0.22f else 0.11f,
)

/** Hover and keyboard-focus fill for rows that are not selected. */
@Composable
@ReadOnlyComposable
internal fun sidebarHighlightColor(): Color = sidebarTint(
    JewelTheme.globalColors.text.normal,
    alpha = if (JewelTheme.isDark) 0.09f else 0.06f,
)

@Composable
@ReadOnlyComposable
internal fun sidebarRowBackground(selected: Boolean, highlighted: Boolean, selectedColor: Color? = null): Color = when {
    selected -> selectedColor ?: sidebarSelectedColor()
    highlighted -> sidebarHighlightColor()
    else -> Color.Transparent
}

@Composable
@ReadOnlyComposable
private fun sidebarTint(color: Color, alpha: Float): Color =
    color.copy(alpha = alpha).compositeOver(JewelTheme.globalColors.toolwindowBackground)

@Composable
@ReadOnlyComposable
internal fun desktopDrawerInsertionColor(): Color = JewelTheme.globalColors.outlines.focused
