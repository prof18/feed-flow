package com.prof18.feedflow.shared.ui.home.components.drawer

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Immutable
data class DrawerItemVisualStyle(
    val itemShape: Shape = CircleShape,
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
