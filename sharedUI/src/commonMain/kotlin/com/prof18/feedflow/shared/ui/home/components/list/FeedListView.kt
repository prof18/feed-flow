package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActionType.NONE
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_BOOKMARK_STATUS
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_READ_STATUS
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewColumn
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import org.jetbrains.compose.ui.tooling.preview.Preview

@Suppress("MagicNumber")
@OptIn(FlowPreview::class)
@Composable
internal fun FeedList(
    feedItems: ImmutableList<FeedItem>,
    feedFontSize: FeedFontSizes,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    swipeActions: SwipeActions,
    updateReadStatus: (Int) -> Unit,
    requestMoreItems: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    markAllAsRead: () -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
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
            val swipeBackgroundColor = when (feedLayout) {
                FeedLayout.LIST -> MaterialTheme.colorScheme.surfaceContainerHighest
                FeedLayout.CARD -> MaterialTheme.colorScheme.surfaceContainer
            }

            val swipeToRight = swipeActions.rightSwipeAction.toSwipeAction(
                feedItem = item,
                swipeBackgroundColor = swipeBackgroundColor,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
            )
            val swipeToLeft = swipeActions.leftSwipeAction.toSwipeAction(
                feedItem = item,
                swipeBackgroundColor = swipeBackgroundColor,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
            )

            FeedItemContainer(feedLayout = feedLayout) {
                if (swipeToRight == null && swipeToLeft == null) {
                    FeedItemView(
                        feedItem = item,
                        shareMenuLabel = shareMenuLabel,
                        shareCommentsMenuLabel = shareCommentsMenuLabel,
                        onFeedItemClick = onFeedItemClick,
                        onCommentClick = onCommentClick,
                        onBookmarkClick = onBookmarkClick,
                        onReadStatusClick = onReadStatusClick,
                        feedFontSize = feedFontSize,
                        onShareClick = onShareClick,
                        feedLayout = feedLayout,
                    )
                } else {
                    SwipeableActionsBox(
                        backgroundUntilSwipeThreshold = swipeBackgroundColor,
                        startActions = swipeToRight?.let { listOf(it) }.orEmpty(),
                        endActions = swipeToLeft?.let { listOf(it) }.orEmpty(),
                    ) {
                        FeedItemView(
                            feedItem = item,
                            shareMenuLabel = shareMenuLabel,
                            shareCommentsMenuLabel = shareCommentsMenuLabel,
                            feedLayout = feedLayout,
                            onFeedItemClick = onFeedItemClick,
                            onCommentClick = onCommentClick,
                            onBookmarkClick = onBookmarkClick,
                            onReadStatusClick = onReadStatusClick,
                            feedFontSize = feedFontSize,
                            onShareClick = onShareClick,
                        )
                    }
                }
            }
            if (index == feedItems.size - 1 && currentFeedFilter !is FeedFilter.Read) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
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
            .debounce(timeoutMillis = 2000)
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

@Suppress("ModifierMissing")
@Composable
fun FeedItemContainer(
    feedLayout: FeedLayout,
    content: @Composable () -> Unit,
) {
    when (feedLayout) {
        FeedLayout.LIST -> content()
        FeedLayout.CARD -> {
            Card(
                modifier = Modifier.padding(Spacing.small),
                shape = RoundedCornerShape(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SwipeActionType.toSwipeAction(
    feedItem: FeedItem,
    swipeBackgroundColor: Color,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
): SwipeAction? {
    return when (this) {
        TOGGLE_READ_STATUS -> SwipeAction(
            icon = {
                Icon(
                    modifier = Modifier.padding(Spacing.regular),
                    imageVector = if (feedItem.isRead) {
                        Icons.Default.MarkEmailUnread
                    } else {
                        Icons.Default.MarkEmailRead
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            background = swipeBackgroundColor,
            onSwipe = {
                onReadStatusClick(
                    FeedItemId(feedItem.id),
                    !feedItem.isRead,
                )
            },
        )

        TOGGLE_BOOKMARK_STATUS -> SwipeAction(
            icon = {
                Icon(
                    modifier = Modifier.padding(Spacing.regular),
                    imageVector = if (feedItem.isBookmarked) {
                        Icons.Default.BookmarkRemove
                    } else {
                        Icons.Default.BookmarkAdd
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            background = swipeBackgroundColor,
            onSwipe = {
                onBookmarkClick(
                    FeedItemId(feedItem.id),
                    !feedItem.isBookmarked,
                )
            },
        )

        NONE -> null
    }
}

@Preview
@Composable
internal fun FeedListPreview() {
    PreviewHelper {
        PreviewColumn {
            FeedList(
                feedItems = feedItemsForPreview,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.LIST,
                currentFeedFilter = FeedFilter.Timeline,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                updateReadStatus = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onCommentClick = {},
                markAllAsRead = {},
                onShareClick = {},
            )

            FeedList(
                feedItems = feedItemsForPreview,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.CARD,
                currentFeedFilter = FeedFilter.Timeline,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                updateReadStatus = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onCommentClick = {},
                markAllAsRead = {},
                onShareClick = {},
            )
        }
    }
}
