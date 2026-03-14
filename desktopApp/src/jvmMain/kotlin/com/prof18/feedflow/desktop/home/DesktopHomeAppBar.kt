package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.shared.ui.icons.CloseSidebar
import com.prof18.feedflow.shared.ui.icons.CloseSidebarReversed
import com.prof18.feedflow.shared.ui.icons.OpenSidebar
import com.prof18.feedflow.shared.ui.icons.OpenSidebarReversed
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DesktopHomeAppBar(
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        expandedHeight = toolbarHeight,
        navigationIcon = if (showDrawerMenu) {
            {
                DrawerIcon(
                    onDrawerMenuClick = onDrawerMenuClick,
                    isDrawerOpen = isDrawerOpen,
                )
            }
        } else {
            {}
        },
        title = {
            Row {
                Text(
                    modifier = Modifier
                        .weight(1f, fill = false),
                    text = currentFeedFilter.getTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (currentFeedFilter !is FeedFilter.Read &&
                    currentFeedFilter !is FeedFilter.Bookmarks
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "($unReadCount)")
                }
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = LocalFeedFlowStrings.current.searchButtonContentDescription,
                )
            }
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleClick() },
                    onTap = { onClick() },
                )
            },
    )
}

@Composable
private fun FeedFilter.getTitle(): String =
    when (this) {
        is FeedFilter.Category -> this.feedCategory.title
        is FeedFilter.Source -> this.feedSource.title
        FeedFilter.Timeline -> LocalFeedFlowStrings.current.appName
        FeedFilter.Read -> LocalFeedFlowStrings.current.drawerTitleRead
        FeedFilter.Bookmarks -> LocalFeedFlowStrings.current.drawerTitleBookmarks
        FeedFilter.Uncategorized -> LocalFeedFlowStrings.current.noCategory
    }

@Composable
private fun DrawerIcon(onDrawerMenuClick: () -> Unit, isDrawerOpen: Boolean) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    IconButton(onClick = onDrawerMenuClick) {
        val icon = when {
            isDrawerOpen && isRtl -> CloseSidebarReversed
            isDrawerOpen -> CloseSidebar
            isRtl -> OpenSidebarReversed
            else -> OpenSidebar
        }
        Icon(
            imageVector = icon,
            contentDescription = LocalFeedFlowStrings.current.drawerMenuButtonContentDescription,
        )
    }
}
