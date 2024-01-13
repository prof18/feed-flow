package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.ui.home.components.FeedItemView
import com.prof18.feedflow.ui.home.components.FeedList
import com.prof18.feedflow.ui.preview.FeedFlowPhonePreview
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun FeedWithContentView(
    modifier: Modifier = Modifier,
    pullRefreshState: PullRefreshState,
    feedUpdateStatus: FeedUpdateStatus,
    lazyListState: LazyListState,
    feedItems: ImmutableList<FeedItem>,
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
    requestMoreItems: () -> Unit,
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
                    .padding(horizontal = Spacing.regular),
                text = stringResource(
                    resource = MR.strings.loading_feed_message,
                    feedRefreshCounter,
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
        ) {
            FeedList(
                modifier = Modifier,
                feedItems = feedItems,
                listState = lazyListState,
                updateReadStatus = { index ->
                    updateReadStatus(index)
                },
                requestMoreItems = requestMoreItems,
            ) { feedItem ->
                FeedItemView(
                    feedItem = feedItem,
                    onFeedItemClick = onFeedItemClick,
                    onFeedItemLongClick = onFeedItemLongClick,
                    feedItemImage = { url ->
                        FeedItemImage(
                            modifier = Modifier
                                .padding(start = Spacing.regular),
                            url = url,
                            size = 96.dp,
                        )
                    },
                )
            }

            PullRefreshIndicator(
                feedUpdateStatus.isLoading(),
                pullRefreshState,
                Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@FeedFlowPhonePreview
@Composable
private fun FeedWithContentViewPreview() {
    FeedFlowTheme {
        FeedWithContentView(
            feedUpdateStatus = inProgressFeedUpdateStatus,
            pullRefreshState = rememberPullRefreshState(
                refreshing = false,
                onRefresh = { },
            ),
            feedItems = feedItemsForPreview,
            lazyListState = LazyListState(),
            updateReadStatus = {},
            onFeedItemClick = {},
            onFeedItemLongClick = {},
            requestMoreItems = {},
        )
    }
}
