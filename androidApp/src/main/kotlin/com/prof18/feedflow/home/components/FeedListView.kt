package com.prof18.feedflow.home.components

import FeedFlowTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.presentation.model.FeedItemClickedInfo
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.ui.theme.Spacing
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(FlowPreview::class)
@Composable
internal fun FeedList(
    modifier: Modifier = Modifier,
    feedItems: List<FeedItem>,
    listState: LazyListState = rememberLazyListState(),
    updateReadStatus: (Int) -> Unit,
    onFeedItemClick: (FeedItemClickedInfo) -> Unit,
    onFeedItemLongClick: (FeedItemClickedInfo) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        items(
            items = feedItems,
        ) { item ->
            FeedItemView(
                feedItem = item,
                onFeedItemClick = onFeedItemClick,
                onFeedItemLongClick = onFeedItemLongClick,
            )
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
private fun FeedItemView(
    feedItem: FeedItem,
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
                            url = feedItem.url
                        )
                    )
                },
                onLongClick = if (feedItem.commentsUrl != null) {
                    {
                        onFeedItemLongClick(
                            FeedItemClickedInfo(
                                id = feedItem.id,
                                url = feedItem.commentsUrl!!,
                            )
                        )
                    }
                } else {
                    null
                }
            )
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small)
    ) {

        Text(
            text = feedItem.feedSource.title,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
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
                FeedItemImage(
                    modifier = Modifier
                        .padding(start = Spacing.regular),
                    url = url,
                    width = 96.dp,
                )
            }
        }

        Text(
            modifier = Modifier
                .padding(top = Spacing.small),
            text = feedItem.dateString,
            style = MaterialTheme.typography.bodySmall
        )

        Divider(
            modifier = Modifier
                .padding(top = Spacing.regular),
            thickness = 0.2.dp,
            color = Color.Gray,
        )
    }
}

@Composable
private fun FeedItemImage(
    modifier: Modifier = Modifier,
    url: String,
    width: Dp
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier
                .size(width)
                .background(Color.Green),
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .wrapContentHeight()
                .width(width)
                .clip(RoundedCornerShape(Spacing.small))
        )
    }
}

@FeedFlowPreview
@Composable
private fun FeedListPreview() {
    FeedFlowTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            FeedList(
                feedItems = feedItemsForPreview,
                updateReadStatus = {},
                onFeedItemClick = {},
                onFeedItemLongClick = {},
            )
        }
    }
}