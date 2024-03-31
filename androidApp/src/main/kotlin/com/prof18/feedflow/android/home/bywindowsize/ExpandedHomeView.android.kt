package com.prof18.feedflow.android.home.bywindowsize

import FeedFlowTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.home.components.HomeScaffold
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.preview.PreviewTablet
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun ExpandedHomeView(
    feedItems: ImmutableList<FeedItem>,
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    snackbarHostState: SnackbarHostState,
    feedUpdateStatus: FeedUpdateStatus,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onDeleteDatabaseClick: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    refreshData: () -> Unit,
    requestNewData: () -> Unit,
    forceRefreshData: () -> Unit,
    markAsReadOnScroll: (Int) -> Unit,
    markAsRead: (FeedItemId) -> Unit,
    markAllRead: () -> Unit,
    openUrl: (String) -> Unit,
    updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    updateReadStatus: (FeedItemId, Boolean) -> Unit,
    onBackToTimelineClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = feedUpdateStatus.isLoading(),
        onRefresh = refreshData,
    )

    val isDrawerHidden = currentFeedFilter is FeedFilter.Timeline && feedItems.isEmpty() && navDrawerState.isEmpty()

    Row {
        if (!isDrawerHidden) {
            Scaffold(
                modifier = Modifier
                    .weight(1f),
            ) { paddingValues ->
                Drawer(
                    modifier = Modifier
                        .padding(paddingValues),
                    navDrawerState = navDrawerState,
                    currentFeedFilter = currentFeedFilter,
                    onFeedFilterSelected = { feedFilter ->
                        onFeedFilterSelected(feedFilter)
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                )
            }
        }

        HomeScaffold(
            modifier = Modifier
                .weight(2f),
            unReadCount = unReadCount,
            onSettingsButtonClicked = onSettingsButtonClicked,
            scope = scope,
            listState = listState,
            snackbarHostState = snackbarHostState,
            loadingState = feedUpdateStatus,
            feedState = feedItems,
            pullRefreshState = pullRefreshState,
            currentFeedFilter = currentFeedFilter,
            onAddFeedClick = onAddFeedClick,
            refreshData = refreshData,
            requestNewData = requestNewData,
            forceRefreshData = forceRefreshData,
            onDeleteDatabaseClick = onDeleteDatabaseClick,
            markAsReadOnScroll = markAsReadOnScroll,
            markAsRead = markAsRead,
            markAllRead = markAllRead,
            onClearOldArticlesClicked = onClearOldArticlesClicked,
            onDrawerMenuClick = {},
            openUrl = openUrl,
            updateReadStatus = updateReadStatus,
            updateBookmarkStatus = updateBookmarkStatus,
            onBackToTimelineClick = onBackToTimelineClick,
        )
    }
}

@PreviewTablet
@Composable
private fun ExpandedHomeViewPreview() {
    FeedFlowTheme {
        ExpandedHomeView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            snackbarHostState = SnackbarHostState(),
            feedUpdateStatus = inProgressFeedUpdateStatus,
            currentFeedFilter = FeedFilter.Timeline,
            onSettingsButtonClicked = {},
            onAddFeedClick = {},
            onClearOldArticlesClicked = {},
            refreshData = {},
            requestNewData = {},
            forceRefreshData = {},
            onDeleteDatabaseClick = {},
            markAsReadOnScroll = {},
            markAsRead = {},
            markAllRead = {},
            onFeedFilterSelected = {},
            openUrl = {},
            updateReadStatus = { _, _ -> },
            updateBookmarkStatus = { _, _ -> },
            onBackToTimelineClick = {},
        )
    }
}
