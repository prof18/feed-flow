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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemType
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
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
    feedFontSizes: FeedFontSizes,
    feedItemType: FeedItemType,
    currentFeedFilter: FeedFilter,
    swipeActions: SwipeActions,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    requestMoreItems: () -> Unit,
    onRefresh: () -> Unit,
    markAllAsRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        AnimatedVisibility(feedUpdateStatus.isLoading()) {
            val feedRefreshCounter = if (feedUpdateStatus.refreshedFeedCount > 0 &&
                feedUpdateStatus.totalFeedCount > 0
            ) {
                "${feedUpdateStatus.refreshedFeedCount}/${feedUpdateStatus.totalFeedCount}"
            } else {
                "..."
            }

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
        val context = LocalContext.current

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
                currentFeedFilter = currentFeedFilter,
                shareCommentsMenuLabel = LocalFeedFlowStrings.current.menuShareComments,
                shareMenuLabel = LocalFeedFlowStrings.current.menuShare,
                swipeActions = swipeActions,
                onFeedItemClick = onFeedItemClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onCommentClick = onCommentClick,
                updateReadStatus = { index ->
                    updateReadStatus(index)
                },
                requestMoreItems = requestMoreItems,
                markAllAsRead = markAllAsRead,
                feedFontSize = feedFontSizes,
                onShareClick = { titleAndUrl ->
                    context.openShareSheet(
                        title = titleAndUrl.title,
                        url = titleAndUrl.url,
                    )
                },
                feedItemType = feedItemType,
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
            feedFontSizes = FeedFontSizes(),
            feedItemType = FeedItemType.LIST_TILE,
            currentFeedFilter = FeedFilter.Timeline,
            swipeActions = SwipeActions(
                leftSwipeAction = SwipeActionType.NONE,
                rightSwipeAction = SwipeActionType.NONE,
            ),
            updateReadStatus = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            requestMoreItems = {},
            onRefresh = {},
            markAllAsRead = {},
        )
    }
}
