package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.core.ScrollAreaScope
import com.composables.core.Thumb
import com.composables.core.ThumbVisibility
import com.composables.core.VerticalScrollbar
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

@Composable
fun ScrollAreaScope.FeedFlowVerticalScrollbar(
    modifier: Modifier = Modifier,
) {
    VerticalScrollbar(
        modifier = modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(12.dp),
    ) {
        Thumb(
            modifier = Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
            thumbVisibility = ThumbVisibility.AlwaysVisible,
        )
    }
}
