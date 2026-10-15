package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState

@Composable
@Suppress("UnusedParameter")
internal fun DesktopHomeToolbar(
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
    HomeToolbar(
        currentFeedFilter = currentFeedFilter,
        unReadCount = unReadCount,
        isUnreadCountHidden = isUnreadCountHidden,
        showDrawerMenu = showDrawerMenu,
        isDrawerOpen = isDrawerOpen,
        onDrawerMenuClick = onDrawerMenuClick,
        onClick = onClick,
        onDoubleClick = onDoubleClick,
        onSearchClick = onSearchClick,
        onRefreshClick = onRefreshClick,
        isRefreshing = isRefreshing,
        refreshEnabled = refreshEnabled,
        modifier = modifier,
    )
}

@Composable
internal fun DesktopFeedSidebar(
    displayState: HomeDisplayState,
    feedManagementActions: FeedManagementActions,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FeedSidebar(
        displayState = displayState,
        feedManagementActions = feedManagementActions,
        onFeedFilterSelected = onFeedFilterSelected,
        modifier = modifier,
    )
}
