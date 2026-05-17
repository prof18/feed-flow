package com.prof18.feedflow.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.icons.CloseSidebar
import com.prof18.feedflow.shared.ui.icons.CloseSidebarReversed
import com.prof18.feedflow.shared.ui.icons.OpenSidebar
import com.prof18.feedflow.shared.ui.icons.OpenSidebarReversed
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFloatingToolbar(
    displayState: HomeDisplayState,
    showDrawerMenu: Boolean,
    isDrawerOpen: Boolean,
    onDrawerMenuClick: () -> Unit,
    onMarkAllReadClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onForceRefreshClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onBackupClick: () -> Unit,
    viewMenuState: HomeViewMenuState,
    onFeedOrderChange: (FeedOrder) -> Unit,
    onShowReadArticlesTimelineChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showViewOptionsSheet by remember { mutableStateOf(false) }
    val viewOptionsSheetState = rememberModalBottomSheetState()
    val strings = LocalFeedFlowStrings.current
    val currentFeedFilter = displayState.currentFeedFilter

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
            .padding(top = FloatingToolbarDefaults.ScreenOffset),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f, fill = false)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onDoubleTap = { onDoubleClick() },
                    )
                },
            shape = FloatingToolbarDefaults.ContainerShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showDrawerMenu) {
                    DrawerIcon(
                        onDrawerMenuClick = onDrawerMenuClick,
                        isDrawerOpen = isDrawerOpen,
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            ),
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                val showCount = !displayState.isUnreadCountHidden &&
                    currentFeedFilter !is FeedFilter.Read &&
                    currentFeedFilter !is FeedFilter.Bookmarks

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier = Modifier.widthIn(max = 180.dp),
                        text = currentFeedFilter.getTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (showCount) {
                        Text(
                            text = displayState.unReadCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            shape = FloatingToolbarDefaults.ContainerShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 6.dp,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = strings.searchButtonContentDescription,
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = strings.moreOptionsButtonContentDescription,
                        )
                    }

                    HomeAppBarDropdownMenu(
                        showMenu = showMenu,
                        feedFilter = currentFeedFilter,
                        closeMenu = { showMenu = false },
                        onMarkAllReadClicked = onMarkAllReadClicked,
                        onClearOldArticlesClicked = onClearOldArticlesClicked,
                        onForceRefreshClick = onForceRefreshClick,
                        onEditFeedClick = { feedSource ->
                            showMenu = false
                            onEditFeedClick(feedSource)
                        },
                        isSyncUploadRequired = displayState.isSyncUploadRequired,
                        onBackupClick = {
                            showMenu = false
                            onBackupClick()
                        },
                        onViewOptionsClick = { showViewOptionsSheet = true },
                    )
                }
            }
        }
    }

    if (showViewOptionsSheet) {
        HomeViewOptionsBottomSheet(
            state = viewMenuState,
            onFeedOrderChange = onFeedOrderChange,
            onShowReadArticlesTimelineChange = onShowReadArticlesTimelineChange,
            onDismiss = { showViewOptionsSheet = false },
            sheetState = viewOptionsSheetState,
        )
    }
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
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
