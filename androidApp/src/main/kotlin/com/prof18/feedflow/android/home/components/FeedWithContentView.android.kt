package com.prof18.feedflow.android.home.components

import FeedFlowTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.ui.home.components.FeedList
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FeedWithContentView(
    feedUpdateStatus: FeedUpdateStatus,
    lazyListState: LazyListState,
    feedItems: ImmutableList<FeedItem>,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    requestMoreItems: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        AnimatedVisibility(feedUpdateStatus.isLoading()) {
            val feedRefreshCounter = """
                    ${feedUpdateStatus.refreshedFeedCount}/${feedUpdateStatus.totalFeedCount}
            """.trimIndent()
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular)
                    .tagForTesting(TestingTag.LOADING_BAR),
                text = LocalFeedFlowStrings.current.loadingFeedMessage(feedRefreshCounter),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        val state = rememberPullToRefreshState()

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = state,
            isRefreshing = feedUpdateStatus.isLoading(),
            onRefresh = onRefresh,
        ) {
            FeedList(
                modifier = Modifier,
                feedItems = feedItems,
                listState = lazyListState,
                onFeedItemClick = onFeedItemClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                updateReadStatus = { index ->
                    updateReadStatus(index)
                },
                requestMoreItems = requestMoreItems,
            )
        }
    }
}

@PreviewPhone
@Composable
private fun FeedWithContentViewPreview() {
    FeedFlowTheme {
        FeedWithContentView(
            feedUpdateStatus = inProgressFeedUpdateStatus,
            lazyListState = LazyListState(),
            feedItems = feedItemsForPreview,
            updateReadStatus = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            requestMoreItems = {},
            onRefresh = {},
        )
    }
}
