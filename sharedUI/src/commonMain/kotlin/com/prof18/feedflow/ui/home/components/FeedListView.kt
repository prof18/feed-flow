package com.prof18.feedflow.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemClickedInfo
import com.prof18.feedflow.ui.style.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("MagicNumber")
@OptIn(FlowPreview::class)
@Composable
fun FeedList(
    modifier: Modifier = Modifier,
    feedItems: List<FeedItem>,
    listState: LazyListState = rememberLazyListState(),
    updateReadStatus: (Int) -> Unit,
    feedItemView: @Composable (FeedItem) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(
            items = feedItems,
        ) { item ->
            feedItemView(item)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(2000)
            .collect { index ->
                if (index > 1) {
                    updateReadStatus(index - 1)
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItemView(
    feedItem: FeedItem,
    feedItemImage: @Composable (String) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
) {
    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    onFeedItemClick(
                        FeedItemClickedInfo(
                            id = feedItem.id,
                            url = feedItem.url,
                        ),
                    )
                },
                onLongClick = if (feedItem.commentsUrl != null) {
                    {
                        onFeedItemLongClick(
                            FeedItemClickedInfo(
                                id = feedItem.id,
                                url = feedItem.commentsUrl!!,
                            ),
                        )
                    }
                } else {
                    null
                },
            )
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small),
    ) {
        Text(
            text = feedItem.feedSource.title,
            style = MaterialTheme.typography.bodySmall,
        )

        TitleSubtitleAndImageRow(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            feedItem = feedItem,
            feedItemImage = feedItemImage,
        )

        feedItem.dateString?.let { dateString ->
            Text(
                modifier = Modifier
                    .padding(top = Spacing.small),
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Divider(
            modifier = Modifier
                .padding(top = Spacing.regular),
            thickness = 0.2.dp,
            color = Color.Gray,
        )
    }
}

@Composable
private fun TitleSubtitleAndImageRow(
    modifier: Modifier = Modifier,
    feedItem: FeedItem,
    feedItemImage: @Composable (String) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
        ) {
            Text(
                modifier = Modifier
                    .padding(top = Spacing.small),
                text = feedItem.title,
                style = MaterialTheme.typography.titleSmall,
            )

            feedItem.subtitle?.let { subtitle ->
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.small),
                    text = subtitle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        feedItem.imageUrl?.let { url ->
            feedItemImage(url)
        }
    }
}
