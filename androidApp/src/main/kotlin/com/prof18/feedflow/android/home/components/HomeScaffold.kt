package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun HomeScaffold(
    unReadCount: Long,
    scope: CoroutineScope,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    loadingState: FeedUpdateStatus,
    feedState: ImmutableList<FeedItem>,
    feedFontSizes: FeedFontSizes,
    currentFeedFilter: FeedFilter,
    swipeActions: SwipeActions,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onDrawerMenuClick: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    refreshData: () -> Unit,
    requestNewData: () -> Unit,
    forceRefreshData: () -> Unit,
    onDeleteDatabaseClick: () -> Unit,
    markAsReadOnScroll: (Int) -> Unit,
    markAsRead: (FeedItemId) -> Unit,
    markAllRead: () -> Unit,
    openUrl: (FeedItemUrlInfo) -> Unit,
    updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    updateReadStatus: (FeedItemId, Boolean) -> Unit,
    onBackToTimelineClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerMenuOpen: Boolean = false,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(
                currentFeedFilter = currentFeedFilter,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerMenuOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                onMarkAllReadClicked = markAllRead,
                onClearOldArticlesClicked = onClearOldArticlesClicked,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        refreshData()
                    }
                },
                onForceRefreshClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        forceRefreshData()
                    }
                },
                onDeleteDatabase = onDeleteDatabaseClick,
                onSearchClick = onSearchClick,
                onEditFeedClick = onEditFeedClick,
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HomeScreenContent(
            paddingValues = padding,
            loadingState = loadingState,
            feedState = feedState,
            listState = listState,
            feedFontSizes = feedFontSizes,
            currentFeedFilter = currentFeedFilter,
            swipeActions = swipeActions,
            onRefresh = {
                refreshData()
            },
            updateReadStatus = markAsReadOnScroll,
            onFeedItemClick = { feedInfo ->
                openUrl(feedInfo)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onCommentClick = { feedInfo ->
                openUrl(feedInfo)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onBookmarkClick = { feedItemId, isBookmarked ->
                updateBookmarkStatus(feedItemId, isBookmarked)
            },
            onReadStatusClick = { feedItemId, isRead ->
                updateReadStatus(feedItemId, isRead)
            },
            onAddFeedClick = onAddFeedClick,
            requestMoreItems = requestNewData,
            onBackToTimelineClick = onBackToTimelineClick,
            markAllAsRead = markAllRead,
            onOpenDrawerClick = onDrawerMenuClick,
            isDrawerVisible = showDrawerMenu,
        )
    }
}

@PreviewPhone
@Composable
private fun HomeScaffoldPreview() {
    FeedFlowTheme {
        HomeScaffold(
            unReadCount = 42,
            scope = rememberCoroutineScope(),
            listState = rememberLazyListState(),
            snackbarHostState = SnackbarHostState(),
            loadingState = InProgressFeedUpdateStatus(
                refreshedFeedCount = 10,
                totalFeedCount = 42,
            ),
            feedState = feedItemsForPreview,
            feedFontSizes = FeedFontSizes(),
            currentFeedFilter = FeedFilter.Timeline,
            swipeActions = SwipeActions(
                leftSwipeAction = SwipeActionType.NONE,
                rightSwipeAction = SwipeActionType.NONE,
            ),
            onAddFeedClick = { },
            onSettingsButtonClicked = { },
            onClearOldArticlesClicked = { },
            refreshData = { },
            requestNewData = { },
            forceRefreshData = { },
            onDeleteDatabaseClick = { },
            markAsReadOnScroll = { },
            markAsRead = { },
            markAllRead = { },
            openUrl = { },
            showDrawerMenu = true,
            isDrawerMenuOpen = false,
            onDrawerMenuClick = {},
            updateReadStatus = { _, _ -> },
            updateBookmarkStatus = { _, _ -> },
            onBackToTimelineClick = {},
            onSearchClick = {},
            onEditFeedClick = { },
        )
    }
}
