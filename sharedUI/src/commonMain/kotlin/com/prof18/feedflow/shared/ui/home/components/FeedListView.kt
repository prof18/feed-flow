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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.feed.LinkOpeningPreferenceSelector
import com.prof18.feedflow.shared.ui.feedsourcelist.feedSourceMenuClickModifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("MagicNumber")
@OptIn(FlowPreview::class)
@Composable
fun FeedList(
    feedItems: ImmutableList<FeedItem>,
    feedFontSize: FeedFontSizes,
    updateReadStatus: (Int) -> Unit,
    requestMoreItems: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    markAllAsRead: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
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
            FeedItemView(
                feedItem = item,
                index = index,
                onFeedItemClick = onFeedItemClick,
                onCommentClick = onCommentClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                feedFontSize = feedFontSize,
            )

            if (index == feedItems.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    TextButton(
                        modifier = Modifier
                            .padding(top = Spacing.small)
                            .padding(bottom = Spacing.medium)
                            .align(Alignment.Center),
                        onClick = markAllAsRead,
                    ) {
                        Text(LocalFeedFlowStrings.current.markAllReadButton)
                    }
                }
            }
        }
    }

    val latestUpdateReadStatus by rememberUpdatedState(updateReadStatus)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(2000)
            .collect { index ->
                if (index > 1) {
                    latestUpdateReadStatus(index - 1)
                }
            }
    }

    val latestRequestMoreItems by rememberUpdatedState(requestMoreItems)
    LaunchedEffect(key1 = shouldStartPaginate.value) {
        if (shouldStartPaginate.value) {
            latestRequestMoreItems()
        }
    }
}

@Composable
fun FeedItemView(
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    index: Int,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showItemMenu by remember {
        mutableStateOf(
            false,
        )
    }

    Column(
        modifier = modifier
            .feedSourceMenuClickModifier(
                onClick = {
                    onFeedItemClick(
                        FeedItemUrlInfo(
                            id = feedItem.id,
                            url = feedItem.url,
                            title = feedItem.title,
                            isBookmarked = feedItem.isBookmarked,
                            linkOpeningPreference = feedItem.feedSource.linkOpeningPreference,
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
        FeedSourceAndUnreadDotRow(
            feedItem = feedItem,
            feedFontSize = feedFontSize,
            index = index,
        )

        TitleSubtitleAndImageRow(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            feedItem = feedItem,
            feedFontSize = feedFontSize,
        )

        feedItem.dateString?.let { dateString ->
            Text(
                modifier = Modifier
                    .padding(top = Spacing.small),
                text = dateString,
                fontSize = feedFontSize.feedMetaFontSize.sp,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        HorizontalDivider(
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
    feedFontSize: FeedFontSizes,
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
            fontSize = feedFontSize.feedMetaFontSize.sp,
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
    feedItem: FeedItem,
    feedFontSize: FeedFontSizes,
    modifier: Modifier = Modifier,
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
                    fontSize = feedFontSize.feedTitleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = (feedFontSize.feedTitleFontSize + 4).sp,
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
                    fontSize = feedFontSize.feedDescFontSize.sp,
                    lineHeight = (feedFontSize.feedDescFontSize + 6).sp,
                    style = MaterialTheme.typography.bodySmall,
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
                LocalFeedFlowStrings.current.menuOpenComments,
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
                    title = feedItem.title,
                    openOnlyOnBrowser = true,
                    isBookmarked = feedItem.isBookmarked,
                    linkOpeningPreference = LinkOpeningPreference.PREFERRED_BROWSER,
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
                    LocalFeedFlowStrings.current.menuRemoveFromBookmark
                } else {
                    LocalFeedFlowStrings.current.menuAddToBookmark
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
                    LocalFeedFlowStrings.current.menuMarkAsUnread
                } else {
                    LocalFeedFlowStrings.current.menuMarkAsRead
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
