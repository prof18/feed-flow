package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.shared.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.home.components.EmptyFeedView
import com.prof18.feedflow.shared.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: ImmutableList<FeedItem>,
    feedFontSizes: FeedFontSizes,
    listState: LazyListState,
    currentFeedFilter: FeedFilter,
    isDrawerVisible: Boolean,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onAddFeedClick: () -> Unit,
    requestMoreItems: () -> Unit,
    onBackToTimelineClick: () -> Unit,
    markAllAsRead: () -> Unit,
    onOpenDrawerClick: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    when {
        loadingState is NoFeedSourcesStatus -> {
            NoFeedsSourceView(
                modifier = Modifier
                    .padding(paddingValues),
                onAddFeedClick = {
                    onAddFeedClick()
                },
            )
        }

        !loadingState.isLoading() && feedState.isEmpty() -> {
            EmptyFeedView(
                modifier = Modifier
                    .padding(paddingValues),
                currentFeedFilter = currentFeedFilter,
                onReloadClick = onRefresh,
                onBackToTimelineClick = onBackToTimelineClick,
                onOpenDrawerClick = onOpenDrawerClick,
                isDrawerVisible = isDrawerVisible,
            )
        }

        else -> FeedWithContentView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            feedUpdateStatus = loadingState,
            feedItems = feedState,
            feedFontSizes = feedFontSizes,
            lazyListState = listState,
            updateReadStatus = updateReadStatus,
            onFeedItemClick = onFeedItemClick,
            requestMoreItems = requestMoreItems,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
            onCommentClick = onCommentClick,
            onRefresh = onRefresh,
            markAllAsRead = markAllAsRead,
        )
    }
}

@PreviewPhone
@Composable
private fun HomeScreeContentLoadingPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(0.dp),
            loadingState = InProgressFeedUpdateStatus(
                refreshedFeedCount = 10,
                totalFeedCount = 42,
            ),
            feedState = feedItemsForPreview,
            feedFontSizes = FeedFontSizes(),
            listState = rememberLazyListState(),
            currentFeedFilter = FeedFilter.Timeline,
            isDrawerVisible = true,
            updateReadStatus = {},
            onFeedItemClick = {},
            onAddFeedClick = {},
            onRefresh = {},
            requestMoreItems = {},
            onCommentClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onBackToTimelineClick = {},
            markAllAsRead = {},
            onOpenDrawerClick = {},
        )
    }
}

@PreviewPhone
@Composable
private fun HomeScreeContentLoadedPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(0.dp),
            loadingState = FinishedFeedUpdateStatus,
            feedState = feedItemsForPreview,
            feedFontSizes = FeedFontSizes(),
            listState = rememberLazyListState(),
            currentFeedFilter = FeedFilter.Timeline,
            isDrawerVisible = true,
            updateReadStatus = {},
            onFeedItemClick = {},
            onAddFeedClick = {},
            requestMoreItems = {},
            onCommentClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onBackToTimelineClick = {},
            markAllAsRead = {},
            onOpenDrawerClick = {},
        )
    }
}
