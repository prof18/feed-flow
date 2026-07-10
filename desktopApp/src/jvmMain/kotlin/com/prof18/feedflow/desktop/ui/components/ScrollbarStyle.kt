package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composeunstyled.ScrollbarState
import com.composeunstyled.Thumb
import com.composeunstyled.ThumbVisibility
import com.composeunstyled.UnstyledVerticalScrollbar
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
fun BoxScope.FeedFlowVerticalScrollbar(
    scrollbarState: ScrollbarState,
    modifier: Modifier = Modifier,
) {
    UnstyledVerticalScrollbar(
        scrollbarState = scrollbarState,
        modifier = modifier
            .align(Alignment.TopEnd)
            .fillMaxHeight()
            .width(12.dp),
    ) {
        Thumb(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
            thumbVisibility = ThumbVisibility.AlwaysVisible,
        )
    }
}
