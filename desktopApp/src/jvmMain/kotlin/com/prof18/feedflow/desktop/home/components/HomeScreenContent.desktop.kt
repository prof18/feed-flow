package com.prof18.feedflow.desktop.home.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.ui.home.components.EmptyFeedView
import com.prof18.feedflow.shared.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: ImmutableList<FeedItem>,
    feedFontSizes: FeedFontSizes,
    listState: LazyListState,
    unReadCount: Long,
    currentFeedFilter: FeedFilter,
    swipeActions: SwipeActions,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onAddFeedClick: () -> Unit,
    requestMoreItems: () -> Unit,
    onBackToTimelineClick: () -> Unit,
    onSearchClick: () -> Unit,
    markAllAsRead: () -> Unit,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerMenuOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(paddingValues),
    ) {
        FeedContentToolbar(
            unReadCount = unReadCount,
            showDrawerMenu = showDrawerMenu,
            isDrawerOpen = isDrawerMenuOpen,
            onDrawerMenuClick = onDrawerMenuClick,
            currentFeedFilter = currentFeedFilter,
            onSearchClick = onSearchClick,
        )

        when {
            loadingState is NoFeedSourcesStatus -> NoFeedsSourceView(
                onAddFeedClick = onAddFeedClick,
            )

            !loadingState.isLoading() && feedState.isEmpty() -> EmptyFeedView(
                currentFeedFilter = currentFeedFilter,
                onReloadClick = onRefresh,
                onBackToTimelineClick = onBackToTimelineClick,
                onOpenDrawerClick = onDrawerMenuClick,
                isDrawerVisible = showDrawerMenu,
            )

            else -> FeedWithContentView(
                paddingValues = paddingValues,
                feedState = feedState,
                loadingState = loadingState,
                listState = listState,
                currentFeedFilter = currentFeedFilter,
                feedFontSizes = feedFontSizes,
                swipeActions = swipeActions,
                updateReadStatus = updateReadStatus,
                onFeedItemClick = onFeedItemClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                requestMoreItems = requestMoreItems,
                markAllAsRead = markAllAsRead,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenContentPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(),
            loadingState = inProgressFeedUpdateStatus,
            feedState = feedItemsForPreview,
            listState = LazyListState(),
            feedFontSizes = FeedFontSizes(),
            unReadCount = 42,
            currentFeedFilter = FeedFilter.Timeline,
            swipeActions = SwipeActions(
                leftSwipeAction = SwipeActionType.NONE,
                rightSwipeAction = SwipeActionType.NONE
            ),
            onRefresh = {},
            updateReadStatus = {},
            onFeedItemClick = {},
            onAddFeedClick = {},
            requestMoreItems = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onBackToTimelineClick = {},
            onSearchClick = {},
            markAllAsRead = {},
        )
    }
}
