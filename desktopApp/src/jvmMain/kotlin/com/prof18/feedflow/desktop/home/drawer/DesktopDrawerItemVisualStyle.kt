package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.foundation.theme.JewelTheme

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
    itemShape = RoundedCornerShape(14.dp),
    itemMinHeight = 44.dp,
    itemVerticalPadding = 2.dp,
    selectedContainerColor = JewelTheme.globalColors.panelBackground,
    selectedContentColor = JewelTheme.globalColors.text.normal,
)

@Composable
internal fun desktopDrawerInsertionColor(): Color = JewelTheme.globalColors.outlines.focused
