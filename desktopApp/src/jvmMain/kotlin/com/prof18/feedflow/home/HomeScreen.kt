package com.prof18.feedflow.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.presentation.model.FeedItemClickedInfo
import com.prof18.feedflow.domain.model.FeedSource
import com.prof18.feedflow.koin
import com.prof18.feedflow.openInBrowser
import com.prof18.feedflow.ui.components.AsyncImage
import com.prof18.feedflow.ui.components.loadImageBitmap
import com.prof18.feedflow.ui.style.Spacing
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen() {

    val databaseHelper = koin.get<DatabaseHelper>()

    // TODO: temporary, until rss-parser will be multiplatform
    val simpleDateFormat = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
    val feeds = databaseHelper.getFeedItems().map {
        it.map { selectedFeed ->
            FeedItem(
                id = selectedFeed.url_hash,
                url = selectedFeed.url,
                title = selectedFeed.title,
                subtitle = selectedFeed.subtitle,
                content = null,
                imageUrl = selectedFeed.image_url,
                feedSource = FeedSource(
                    id = selectedFeed.feed_source_id,
                    url = selectedFeed.feed_source_url,
                    title = selectedFeed.feed_source_title,
                ),
                isRead = selectedFeed.is_read,
                pubDateMillis = selectedFeed.pub_date,
                dateString = simpleDateFormat.format(Date(selectedFeed.pub_date)),
                commentsUrl = selectedFeed.comments_url,
            )
        }
    }.collectAsState(initial = emptyList())


    Scaffold {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(it)
                .padding(end = 4.dp)
        ) {

            val state = rememberLazyListState()

            LazyColumn(Modifier.fillMaxSize().padding(end = 12.dp), state) {
                items(feeds.value) { feedItem ->
                    FeedItemView(
                        feedItem = feedItem,
                        onFeedItemClick = { clickedInfo ->
                            openInBrowser(clickedInfo.url)
                        },
                        onFeedItemLongClick = { clickedInfo ->
                            openInBrowser(clickedInfo.url)
                        },
                    )

                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(
                    scrollState = state
                )
            )
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
                AsyncImage(
                    modifier = Modifier
                        .wrapContentHeight()
                        .width(96.dp)
                        .clip(RoundedCornerShape(Spacing.small)),
                    load = { loadImageBitmap(url) },
                    painterFor = { remember { BitmapPainter(it) } },
                    contentDescription = null,
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
