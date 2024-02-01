package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.feedsourcelist.feedSourceMenuClickModifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import dev.icerock.moko.resources.compose.stringResource
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
    requestMoreItems: () -> Unit,
    feedItemView: @Composable (FeedItem, Int) -> Unit,
) {
    val shouldStartPaginate = remember {
        derivedStateOf {
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false

            val totalItemsCount = listState.layoutInfo.totalItemsCount
            lastVisibleItemIndex >= totalItemsCount - 15
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
    ) {
        itemsIndexed(
            items = feedItems,
        ) { index, item ->
            feedItemView(item, index)
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

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        if (shouldStartPaginate.value) {
            requestMoreItems()
        }
    }
}

@Composable
fun FeedItemView(
    feedItem: FeedItem,
    index: Int,
    feedItemImage: @Composable (String) -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
) {
    var showItemMenu by remember {
        mutableStateOf(
            false,
        )
    }

    Column(
        modifier = Modifier
            .feedSourceMenuClickModifier(
                onClick = {
                    onFeedItemClick(
                        FeedItemUrlInfo(
                            id = feedItem.id,
                            url = feedItem.url,
                        ),
                    )
                },
                onLongClick = {
                    showItemMenu = true
                },
            )
            .padding(horizontal = Spacing.regular)
            .padding(vertical = Spacing.small)
            .tagForTesting(
                tag = "${TestingTag.FEED_ITEM}_$index",
                mergeDescendants = true,
            ),
    ) {
        FeedSourceAndUnreadDotRow(feedItem, index)

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

        FeedItemContextMenu(
            showMenu = showItemMenu,
            closeMenu = {
                showItemMenu = false
            },
            feedItem = feedItem,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
            onCommentClick = onCommentClick,
        )
    }
}

@Composable
private fun FeedSourceAndUnreadDotRow(
    feedItem: FeedItem,
    index: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!feedItem.isRead) {
            UnreadDot(
                modifier = Modifier
                    .padding(
                        bottom = Spacing.small,
                        end = Spacing.small,
                    )
                    .tagForTesting("${TestingTag.UNREAD_DOT}_$index"),
            )
        }

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = Spacing.small),
            text = feedItem.feedSource.title,
            style = MaterialTheme.typography.bodySmall,
        )

        if (feedItem.isBookmarked) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = Spacing.small),
                tint = MaterialTheme.colorScheme.primary,
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
            )
        }
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
            feedItem.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            val paddingTop = when {
                feedItem.title != null -> Spacing.small
                else -> 0.dp
            }

            feedItem.subtitle?.let { subtitle ->
                Text(
                    modifier = Modifier
                        .padding(top = paddingTop),
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

@Composable
private fun UnreadDot(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .tagForTesting(TestingTag.UNREAD_DOT),
    )
}

@Composable
private fun FeedItemContextMenu(
    showMenu: Boolean,
    feedItem: FeedItem,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = closeMenu,
        properties = PopupProperties(),
    ) {
        ChangeReadStatusMenuItem(
            feedItem = feedItem,
            onReadStatusClick = onReadStatusClick,
            closeMenu = closeMenu,
        )

        ChangeBookmarkStatusMenuItem(
            feedItem = feedItem,
            onBookmarkClick = onBookmarkClick,
            closeMenu = closeMenu,
        )

        if (feedItem.commentsUrl != null) {
            OpenCommentsMenuItem(
                feedItem = feedItem,
                closeMenu = closeMenu,
                onCommentClick = onCommentClick,
            )
        }
    }
}

@Composable
private fun OpenCommentsMenuItem(
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    feedItem: FeedItem,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                stringResource(resource = MR.strings.menu_open_comments),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Forum,
                contentDescription = null,
            )
        },
        onClick = {
            onCommentClick(
                FeedItemUrlInfo(
                    id = feedItem.id,
                    url = requireNotNull(feedItem.commentsUrl),
                ),
            )
            closeMenu()
        },
    )
}

@Composable
private fun ChangeBookmarkStatusMenuItem(
    feedItem: FeedItem,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = if (feedItem.isBookmarked) {
                    stringResource(resource = MR.strings.menu_remove_from_bookmark)
                } else {
                    stringResource(resource = MR.strings.menu_add_to_bookmark)
                },
            )
        },
        leadingIcon = {
            if (feedItem.isBookmarked) {
                Icon(
                    imageVector = Icons.Default.BookmarkRemove,
                    contentDescription = null,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = null,
                )
            }
        },
        onClick = {
            onBookmarkClick(
                FeedItemId(feedItem.id),
                !feedItem.isBookmarked,
            )
            closeMenu()
        },
    )
}

@Composable
private fun ChangeReadStatusMenuItem(
    feedItem: FeedItem,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    closeMenu: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = if (feedItem.isRead) {
                    stringResource(resource = MR.strings.menu_mark_as_unread)
                } else {
                    stringResource(resource = MR.strings.menu_mark_as_read)
                },
            )
        },
        leadingIcon = {
            if (feedItem.isRead) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MarkEmailRead,
                    contentDescription = null,
                )
            }
        },
        onClick = {
            onReadStatusClick(
                FeedItemId(feedItem.id),
                !feedItem.isRead,
            )
            closeMenu()
        },
    )
}
