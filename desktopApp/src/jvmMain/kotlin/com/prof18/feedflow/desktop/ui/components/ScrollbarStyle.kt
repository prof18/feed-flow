package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.theme.rememberDesktopDarkTheme

@Composable
internal fun scrollbarStyle(): ScrollbarStyle {
    val isInDarkTheme = rememberDesktopDarkTheme()
    return ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverDurationMillis = 300,
        unhoverColor = if (isInDarkTheme) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        },
        hoverColor = if (isInDarkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.50f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
        },
    )
}
