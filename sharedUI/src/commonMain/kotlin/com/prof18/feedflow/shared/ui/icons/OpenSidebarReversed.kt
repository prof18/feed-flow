package com.prof18.feedflow.shared.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val OpenSidebarReversed: ImageVector
    get() {
        if (_BootstrapLayoutSidebarReverse != null) return _BootstrapLayoutSidebarReverse!!

        _BootstrapLayoutSidebarReverse = ImageVector.Builder(
            name = "layout-sidebar-reverse",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            path(
                fill = SolidColor(Color.Black),
            ) {
                moveTo(16f, 3f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
                horizontalLineTo(2f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
                verticalLineToRelative(10f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(12f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                close()
                moveToRelative(-5f, -1f)
                verticalLineToRelative(12f)
                horizontalLineTo(2f)
                arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
                verticalLineTo(3f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
                close()
                moveToRelative(1f, 0f)
                horizontalLineToRelative(2f)
                arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
                verticalLineToRelative(10f)
                arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
                horizontalLineToRelative(-2f)
                close()
            }
        }.build()

        return _BootstrapLayoutSidebarReverse!!
    }

private var _BootstrapLayoutSidebarReverse: ImageVector? = null
