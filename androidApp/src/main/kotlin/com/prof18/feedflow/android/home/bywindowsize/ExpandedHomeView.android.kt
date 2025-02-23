package com.prof18.feedflow.android.home.bywindowsize

import FeedFlowTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.home.components.HomeScaffold
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
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
    feedFontSizes: FeedFontSizes,
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
    openUrl: (FeedItemUrlInfo) -> Unit,
    updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    updateReadStatus: (FeedItemId, Boolean) -> Unit,
    onBackToTimelineClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Row {
        Scaffold(
            modifier = Modifier
                .weight(1f),
        ) { paddingValues ->
            Drawer(
                modifier = Modifier
                    .padding(paddingValues),
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                onAddFeedClicked = onAddFeedClick,
                onFeedFilterSelected = { feedFilter ->
                    onFeedFilterSelected(feedFilter)
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onEditFeedClick = onEditFeedClick,
                onPinFeedClick = onPinFeedClick,
                onEditCategoryClick = onEditCategoryClick,
            )
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
            feedFontSizes = feedFontSizes,
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
            onSearchClick = onSearchClick,
            onEditFeedClick = onEditFeedClick,
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
            feedFontSizes = FeedFontSizes(),
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
            onSearchClick = {},
            onEditFeedClick = { _ -> },
            onDeleteFeedSourceClick = { _ -> },
            onPinFeedClick = { _ -> },
            onEditCategoryClick = { _, _ -> },
        )
    }
}
