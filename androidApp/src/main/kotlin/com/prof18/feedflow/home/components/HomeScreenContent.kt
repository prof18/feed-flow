@file:OptIn(ExperimentalMaterialApi::class)

package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.domain.model.FinishedFeedUpdateStatus
import com.prof18.feedflow.domain.model.InProgressFeedUpdateStatus
import com.prof18.feedflow.domain.model.NoFeedSourcesStatus
import com.prof18.feedflow.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.ui.home.components.EmptyFeedView
import com.prof18.feedflow.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.ui.preview.FeedFlowPhonePreview
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongParameterList")
@Composable
internal fun HomeScreenContent(
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedState: ImmutableList<FeedItem>,
    pullRefreshState: PullRefreshState,
    listState: LazyListState,
    onRefresh: () -> Unit = {},
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
    onAddFeedClick: () -> Unit,
    requestMoreItems: () -> Unit,
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
                onReloadClick = {
                    onRefresh()
                },
            )
        }

        else -> FeedWithContentView(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            feedUpdateStatus = loadingState,
            pullRefreshState = pullRefreshState,
            feedItems = feedState,
            lazyListState = listState,
            updateReadStatus = updateReadStatus,
            onFeedItemClick = onFeedItemClick,
            onFeedItemLongClick = onFeedItemLongClick,
            requestMoreItems = requestMoreItems,
        )
    }
}

@FeedFlowPhonePreview
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
            pullRefreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
            ),
            listState = rememberLazyListState(),
            updateReadStatus = {},
            onFeedItemClick = {},
            onFeedItemLongClick = {},
            onAddFeedClick = {},
            onRefresh = {},
            requestMoreItems = {},
        )
    }
}

@FeedFlowPhonePreview
@Composable
private fun HomeScreeContentLoadedPreview() {
    FeedFlowTheme {
        HomeScreenContent(
            paddingValues = PaddingValues(0.dp),
            loadingState = FinishedFeedUpdateStatus,
            feedState = feedItemsForPreview,
            pullRefreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
            ),
            listState = rememberLazyListState(),
            updateReadStatus = {},
            onFeedItemClick = {},
            onFeedItemLongClick = {},
            onAddFeedClick = {},
            requestMoreItems = {},
        )
    }
}
