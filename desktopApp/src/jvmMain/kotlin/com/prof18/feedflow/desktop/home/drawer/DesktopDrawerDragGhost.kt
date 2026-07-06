package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.ui.style.Spacing
import kotlin.math.roundToInt

private const val DragGhostPointerOffsetDp = 12

@Composable
internal fun DragGhost(
    dragState: DesktopDrawerDragState,
    drawerCoordinates: LayoutCoordinates?,
) {
    val payload = dragState.dragPayload ?: return
    if (payload is DrawerDragPayload.FeedSources && payload.feedSources.isEmpty()) {
        return
    }
    val containerCoordinates = drawerCoordinates ?: return
    val offset = dragState.dragOffsetInContainer(containerCoordinates) ?: return
    val density = LocalDensity.current
    val pointerOffsetPx = with(density) { DragGhostPointerOffsetDp.dp.toPx() }
    val offsetX = (offset.x + pointerOffsetPx).roundToInt()
    val offsetY = (offset.y + pointerOffsetPx).roundToInt()

    Surface(
        modifier = Modifier
            .zIndex(1f)
            .offset { IntOffset(offsetX, offsetY) },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        when (payload) {
            is DrawerDragPayload.FeedSources -> {
                val feedSource = payload.feedSources.first()
                FeedSourceDragGhostContent(
                    feedSource = feedSource,
                    dragCount = dragState.dragCount(),
                )
            }
            is DrawerDragPayload.Category -> CategoryDragGhostContent(payload.title)
        }
    }
}

@Composable
private fun FeedSourceDragGhostContent(
    feedSource: FeedSource,
    dragCount: Int,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val imageUrl = feedSource.logoUrl
        if (imageUrl != null) {
            FeedSourceLogoImage(
                size = 20.dp,
                imageUrl = imageUrl,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
            )
        }

        Spacer(Modifier.width(Spacing.small))

        Text(
            text = feedSource.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (dragCount > 1) {
            Spacer(Modifier.width(Spacing.small))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = Spacing.small, vertical = 2.dp),
                    text = dragCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CategoryDragGhostContent(title: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Category,
            contentDescription = null,
        )

        Spacer(Modifier.width(Spacing.small))

        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
