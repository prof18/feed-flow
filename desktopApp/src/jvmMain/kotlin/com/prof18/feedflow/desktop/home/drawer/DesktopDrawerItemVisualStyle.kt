package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
internal fun drawerItemColors(
    style: DrawerItemVisualStyle,
): NavigationDrawerItemColors {
    val selectedContainerColor = style.selectedContainerColor
    val selectedContentColor = style.selectedContentColor

    return if (selectedContainerColor != null && selectedContentColor != null) {
        NavigationDrawerItemDefaults.colors(
            selectedContainerColor = selectedContainerColor,
            selectedIconColor = selectedContentColor,
            selectedTextColor = selectedContentColor,
            selectedBadgeColor = selectedContentColor,
            unselectedContainerColor = Color.Transparent,
        )
    } else {
        NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent,
        )
    }
}

@Composable
internal fun desktopDrawerItemVisualStyle(): DrawerItemVisualStyle {
    val colorScheme = MaterialTheme.colorScheme
    val selectedAlpha = if (colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD) {
        DARK_MODE_DRAWER_SELECTION_ALPHA
    } else {
        LIGHT_MODE_DRAWER_SELECTION_ALPHA
    }

    return DrawerItemVisualStyle(
        itemShape = RoundedCornerShape(14.dp),
        itemMinHeight = 44.dp,
        itemVerticalPadding = 2.dp,
        selectedContainerColor = colorScheme.onSurface.copy(alpha = selectedAlpha),
        selectedContentColor = colorScheme.onSurface,
    )
}

private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f
private const val DARK_MODE_DRAWER_SELECTION_ALPHA = 0.14f
private const val LIGHT_MODE_DRAWER_SELECTION_ALPHA = 0.1f
