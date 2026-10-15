package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktop.ui.components.DesktopDesignTokens
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import kotlin.math.roundToInt

private const val DragGhostPointerOffsetDp = 12

@Composable
internal fun DragGhost(dragState: DesktopDrawerDragState, drawerCoordinates: LayoutCoordinates?) {
    val payload = dragState.dragPayload ?: return
    if (payload is DrawerDragPayload.FeedSources && payload.feedSources.isEmpty()) return
    val coordinates = drawerCoordinates ?: return
    val offset = dragState.dragOffsetInContainer(coordinates) ?: return
    val pointerOffset = with(LocalDensity.current) { DragGhostPointerOffsetDp.dp.toPx() }
    Row(
        modifier = Modifier.zIndex(
            1f,
        ).offset { IntOffset((offset.x + pointerOffset).roundToInt(), (offset.y + pointerOffset).roundToInt()) }
            .clip(RoundedCornerShape(9.dp)).background(JewelTheme.globalColors.panelBackground)
            .padding(horizontal = DesktopDesignTokens.spaceSection, vertical = DesktopDesignTokens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (payload) {
            is DrawerDragPayload.FeedSources -> {
                val source = payload.feedSources.first()
                FeedSourceDragGhostContent(source, dragState.dragCount())
            }
            is DrawerDragPayload.Category -> CategoryDragGhostContent(payload.title)
        }
    }
}

@Composable
private fun FeedSourceDragGhostContent(feedSource: FeedSource, dragCount: Int) {
    val url = feedSource.logoUrl
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (url != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(url).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)),
            )
        } else {
            Icon(AllIconsKeys.Nodes.Bookmark, null)
        }
        Spacer(Modifier.width(DesktopDesignTokens.spaceMedium))
        Text(feedSource.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (dragCount > 1) {
            Spacer(Modifier.width(DesktopDesignTokens.spaceMedium))
            Text(
                dragCount.toString(),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(JewelTheme.globalColors.outlines.focused.copy(alpha = 0.16f))
                    .padding(horizontal = DesktopDesignTokens.spaceMedium, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CategoryDragGhostContent(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(AllIconsKeys.Nodes.Folder, null)
        Spacer(Modifier.width(DesktopDesignTokens.spaceMedium))
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
