package com.prof18.feedflow.desktop.home.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.ui.home.components.EmptyFeedView
import com.prof18.feedflow.shared.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme

@Composable
internal fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: List<FeedItem>,
    listState: LazyListState,
    unReadCount: Long,
    showDrawerMenu: Boolean = false,
    isDrawerMenuOpen: Boolean = false,
    currentFeedFilter: FeedFilter,
    modifier: Modifier = Modifier,
    onDrawerMenuClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onAddFeedClick: () -> Unit,
    requestMoreItems: () -> Unit,
    onBackToTimelineClick: () -> Unit,
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
        )

        when {
            loadingState is NoFeedSourcesStatus -> NoFeedsSourceView(
                onAddFeedClick = onAddFeedClick,
            )

            !loadingState.isLoading() && feedState.isEmpty() -> EmptyFeedView(
                currentFeedFilter = currentFeedFilter,
                onReloadClick = onRefresh,
                onBackToTimelineClick = onBackToTimelineClick,
            )

            else -> FeedWithContentView(
                paddingValues = paddingValues,
                feedState = feedState,
                loadingState = loadingState,
                listState = listState,
                updateReadStatus = updateReadStatus,
                onFeedItemClick = onFeedItemClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                requestMoreItems = requestMoreItems,
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
            unReadCount = 42,
            currentFeedFilter = FeedFilter.Timeline,
            onRefresh = {},
            updateReadStatus = {},
            onFeedItemClick = {},
            onAddFeedClick = {},
            requestMoreItems = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onBackToTimelineClick = {},
        )
    }
}
