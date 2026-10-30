@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.home.toolbarHeight
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip

@Composable
internal fun HomeToolbar(
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    isUnreadCountHidden: Boolean,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier,
    refreshEnabled: Boolean = !isRefreshing,
) {
    val strings = LocalFeedFlowStrings.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val canRefresh by rememberUpdatedState(refreshEnabled && !isRefreshing)
    val isMacOS = getDesktopOS().isMacOs()
    val refreshDescription = strings.refreshFeeds + if (isMacOS) " (⌘R)" else " (F5)"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(toolbarHeight)
            .background(JewelTheme.globalColors.panelBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnClick() },
                    onDoubleTap = { if (canRefresh) currentOnDoubleClick() else currentOnClick() },
                )
            }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDrawerMenu) {
            ToolbarIconButton(
                icon = Icons.Outlined.ViewSidebar,
                description = strings.drawerMenuButtonContentDescription,
                onClick = onDrawerMenuClick,
                selected = isDrawerOpen,
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentFeedFilter.title(
                    strings.appName,
                    strings.drawerTitleRead,
                    strings.drawerTitleBookmarks,
                    strings.noCategory,
                ),
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (
                !isUnreadCountHidden &&
                unReadCount > 0 &&
                currentFeedFilter !is FeedFilter.Read &&
                currentFeedFilter !is FeedFilter.Bookmarks
            ) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = unReadCount.toString(), color = JewelTheme.globalColors.text.info)
            }
        }

        ToolbarIconButton(
            icon = Icons.Outlined.Refresh,
            description = refreshDescription,
            onClick = onRefreshClick,
            enabled = refreshEnabled && !isRefreshing,
        )
        ToolbarIconButton(
            icon = Icons.Outlined.Search,
            description = strings.searchButtonContentDescription,
            onClick = onSearchClick,
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean? = null,
) {
    Tooltip(tooltip = { Text(text = description) }) {
        val content: @Composable () -> Unit = {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = description,
                tint = JewelTheme.globalColors.text.normal,
                modifier = Modifier.size(20.dp),
            )
        }
        // Toggle state is exposed to accessibility only: a filled chip on a toolbar reads as a stuck button.
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(36.dp).semantics { if (selected != null) this.selected = selected },
        ) { content() }
    }
}

private fun FeedFilter.title(
    appName: String,
    readTitle: String,
    bookmarksTitle: String,
    noCategory: String,
): String =
    when (this) {
        is FeedFilter.Category -> feedCategory.title
        is FeedFilter.Source -> feedSource.title
        FeedFilter.Timeline -> appName
        FeedFilter.Read -> readTitle
        FeedFilter.Bookmarks -> bookmarksTitle
        FeedFilter.Uncategorized -> noCategory
    }
