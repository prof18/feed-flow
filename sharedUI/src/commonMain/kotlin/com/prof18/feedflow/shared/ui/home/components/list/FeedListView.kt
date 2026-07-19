package com.prof18.feedflow.shared.ui.home.components.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActionType.NONE
import com.prof18.feedflow.core.model.SwipeActionType.OPEN_IN_BROWSER
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_BOOKMARK_STATUS
import com.prof18.feedflow.core.model.SwipeActionType.TOGGLE_READ_STATUS
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.VisibleFeedItem
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState.NextFeedDisplayEnabledState
import com.prof18.feedflow.shared.ui.preview.feedItemsForPreview
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewColumn
import com.prof18.feedflow.shared.ui.utils.PreviewHelper
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed

val FeedListMaxContentWidth = 720.dp

private val GridContentPadding = Spacing.regular
private val GridMinCellWidth = 280.dp

@OptIn(ExperimentalFoundationApi::class)
@Suppress("MagicNumber")
@Composable
fun FeedList(
    feedItems: ImmutableList<FeedItem>,
    nextFeedState: NextFeedDisplayState,
    feedFontSize: FeedFontSizes,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    swipeActions: SwipeActions,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
    requestMoreItems: () -> Unit,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    markAllAsRead: () -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onNavigateNext: () -> Unit,
    modifier: Modifier = Modifier,
    onGridArrangementChanged: (Boolean) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    showNextFeedButton: Boolean = false,
    isGridLayoutEnabled: Boolean = true,
    isGridLayoutAllowed: Boolean = true,
    onMarkAllAboveAsRead: (String) -> Unit = {},
    onMarkAllBelowAsRead: (String) -> Unit = {},
    feedItemDisplaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
) {
    val itemFeedLayout = feedLayout.normalizeForFeedList()
    val feedBackgroundModifier = if (itemFeedLayout.usesCardBackground() && feedItems.isNotEmpty()) {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    } else {
        Modifier
    }

    BoxWithConstraints(modifier = modifier.then(feedBackgroundModifier)) {
        val gridContentPadding = contentPadding.withHorizontal(GridContentPadding)
        val isGridArrangement = isGridLayoutEnabled &&
            isGridLayoutAllowed &&
            itemFeedLayout.supportsGridArrangement() &&
            maxWidth >= itemFeedLayout.gridMinContentWidth()
        val latestOnGridArrangementChanged by rememberUpdatedState(onGridArrangementChanged)
        LaunchedEffect(isGridArrangement) {
            latestOnGridArrangementChanged(isGridArrangement)
        }
        val shouldStartPaginate = remember(isGridArrangement) {
            derivedStateOf {
                if (isGridArrangement) {
                    val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: return@derivedStateOf false
                    val totalItemsCount = gridState.layoutInfo.totalItemsCount
                    lastVisibleItemIndex >= totalItemsCount - 15
                } else {
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                        ?: return@derivedStateOf false
                    val totalItemsCount = listState.layoutInfo.totalItemsCount
                    lastVisibleItemIndex >= totalItemsCount - 15
                }
            }
        }

        PullToNextLayout(
            onNavigateNext = { onNavigateNext() },
            enabled = !showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState,
            indicator = { progress ->
                PullToNextIndicator(
                    progress = progress,
                    title = (nextFeedState as? NextFeedDisplayEnabledState)?.title,
                )
            },
        ) {
            if (isGridArrangement) {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    columns = StaggeredGridCells.Adaptive(minSize = itemFeedLayout.gridMinCellWidth()),
                    contentPadding = gridContentPadding,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
                    verticalItemSpacing = Spacing.regular,
                ) {
                    staggeredItemsIndexed(
                        items = feedItems,
                    ) { _, item ->
                        FeedItemContainer(
                            feedLayout = itemFeedLayout,
                            isGridCell = true,
                        ) { itemModifier ->
                            FeedItemView(
                                modifier = itemModifier,
                                feedItem = item,
                                shareMenuLabel = shareMenuLabel,
                                shareCommentsMenuLabel = shareCommentsMenuLabel,
                                onFeedItemClick = onFeedItemClick,
                                onCommentClick = onCommentClick,
                                onBookmarkClick = onBookmarkClick,
                                onReadStatusClick = onReadStatusClick,
                                feedFontSize = feedFontSize,
                                onOpenFeedSettings = onOpenFeedSettings,
                                onOpenFeedWebsite = onOpenFeedWebsite,
                                onShareClick = onShareClick,
                                feedLayout = itemFeedLayout,
                                isGridCell = true,
                                currentFeedFilter = currentFeedFilter,
                                onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                                onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                                feedItemDisplaySettings = feedItemDisplaySettings,
                            )
                        }
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        FeedFooterButtons(
                            currentFeedFilter = currentFeedFilter,
                            showNextFeedButton = showNextFeedButton,
                            nextFeedState = nextFeedState,
                            markAllAsRead = markAllAsRead,
                            onNavigateNext = onNavigateNext,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    itemsIndexed(
                        items = feedItems,
                    ) { index, item ->
                        FeedListItem(
                            item = item,
                            feedFontSize = feedFontSize,
                            feedLayout = itemFeedLayout,
                            currentFeedFilter = currentFeedFilter,
                            shareMenuLabel = shareMenuLabel,
                            shareCommentsMenuLabel = shareCommentsMenuLabel,
                            swipeActions = swipeActions,
                            onFeedItemClick = onFeedItemClick,
                            onOpenInBrowser = onOpenInBrowser,
                            onBookmarkClick = onBookmarkClick,
                            onReadStatusClick = onReadStatusClick,
                            onCommentClick = onCommentClick,
                            onShareClick = onShareClick,
                            onOpenFeedSettings = onOpenFeedSettings,
                            onOpenFeedWebsite = onOpenFeedWebsite,
                            onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                            onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                            feedItemDisplaySettings = feedItemDisplaySettings,
                        )
                        if (index == feedItems.size - 1) {
                            FeedFooterButtons(
                                currentFeedFilter = currentFeedFilter,
                                showNextFeedButton = showNextFeedButton,
                                nextFeedState = nextFeedState,
                                markAllAsRead = markAllAsRead,
                                onNavigateNext = onNavigateNext,
                            )
                        }
                    }
                }
            }
        }

        if (isGridArrangement) {
            ObserveVisibleGridFeedItems(
                feedItems = feedItems,
                gridState = gridState,
                onVisibleFeedItemsChanged = onVisibleFeedItemsChanged,
            )
        } else {
            ObserveVisibleFeedItems(
                feedItems = feedItems,
                listState = listState,
                onVisibleFeedItemsChanged = onVisibleFeedItemsChanged,
            )
        }

        val latestRequestMoreItems by rememberUpdatedState(requestMoreItems)
        LaunchedEffect(key1 = shouldStartPaginate.value) {
            if (shouldStartPaginate.value) {
                latestRequestMoreItems()
            }
        }
    }
}

@Composable
private fun FeedListItem(
    item: FeedItem,
    feedFontSize: FeedFontSizes,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    shareMenuLabel: String,
    shareCommentsMenuLabel: String,
    swipeActions: SwipeActions,
    onFeedItemClick: (FeedItemUrlInfo) -> Unit,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onCommentClick: (FeedItemUrlInfo) -> Unit,
    onShareClick: (FeedItemUrlTitle) -> Unit,
    onOpenFeedSettings: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
    onOpenFeedWebsite: (String) -> Unit,
    onMarkAllAboveAsRead: (String) -> Unit,
    onMarkAllBelowAsRead: (String) -> Unit,
    feedItemDisplaySettings: FeedItemDisplaySettings,
) {
    val swipeBackgroundColor = when (feedLayout) {
        FeedLayout.LIST -> MaterialTheme.colorScheme.surfaceContainerHighest
        FeedLayout.CARD,
        FeedLayout.BIG_IMAGE,
        FeedLayout.GRID,
        -> MaterialTheme.colorScheme.surfaceContainer
    }

    val swipeToRight = remember(
        item,
        swipeActions.rightSwipeAction,
        swipeBackgroundColor,
        onOpenInBrowser,
        onBookmarkClick,
        onReadStatusClick,
    ) {
        swipeActions.rightSwipeAction.toSwipeAction(
            feedItem = item,
            swipeBackgroundColor = swipeBackgroundColor,
            onOpenInBrowser = onOpenInBrowser,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
        )
    }
    val swipeToLeft = remember(
        item,
        swipeActions.leftSwipeAction,
        swipeBackgroundColor,
        onOpenInBrowser,
        onBookmarkClick,
        onReadStatusClick,
    ) {
        swipeActions.leftSwipeAction.toSwipeAction(
            feedItem = item,
            swipeBackgroundColor = swipeBackgroundColor,
            onOpenInBrowser = onOpenInBrowser,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
        )
    }
    val startSwipeActions = remember(swipeToRight) {
        swipeToRight?.let { listOf(it) }.orEmpty()
    }
    val endSwipeActions = remember(swipeToLeft) {
        swipeToLeft?.let { listOf(it) }.orEmpty()
    }

    FeedItemContainer(feedLayout = feedLayout) { itemModifier ->
        if (swipeToRight == null && swipeToLeft == null) {
            FeedItemView(
                modifier = itemModifier,
                feedItem = item,
                shareMenuLabel = shareMenuLabel,
                shareCommentsMenuLabel = shareCommentsMenuLabel,
                onFeedItemClick = onFeedItemClick,
                onCommentClick = onCommentClick,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                feedFontSize = feedFontSize,
                onOpenFeedSettings = onOpenFeedSettings,
                onOpenFeedWebsite = onOpenFeedWebsite,
                onShareClick = onShareClick,
                feedLayout = feedLayout,
                currentFeedFilter = currentFeedFilter,
                onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                feedItemDisplaySettings = feedItemDisplaySettings,
            )
        } else {
            SwipeableActionsBox(
                modifier = itemModifier,
                backgroundUntilSwipeThreshold = swipeBackgroundColor,
                startActions = startSwipeActions,
                endActions = endSwipeActions,
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
                    onOpenFeedSettings = onOpenFeedSettings,
                    onOpenFeedWebsite = onOpenFeedWebsite,
                    onShareClick = onShareClick,
                    currentFeedFilter = currentFeedFilter,
                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                    feedItemDisplaySettings = feedItemDisplaySettings,
                )
            }
        }
    }
}

@Composable
private fun FeedFooterButtons(
    currentFeedFilter: FeedFilter,
    showNextFeedButton: Boolean,
    nextFeedState: NextFeedDisplayState,
    markAllAsRead: () -> Unit,
    onNavigateNext: () -> Unit,
) {
    if (currentFeedFilter !is FeedFilter.Read) {
        MarkAllReadButton(
            showNextFeedButton = showNextFeedButton,
            nextFeedState = nextFeedState,
            onClick = markAllAsRead,
        )
    }
    if (showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState) {
        NavigateNextButton(
            title = nextFeedState.title,
            onClick = onNavigateNext,
        )
    }
}

@Composable
private fun ObserveVisibleFeedItems(
    feedItems: ImmutableList<FeedItem>,
    listState: LazyListState,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
) {
    val latestFeedItems by rememberUpdatedState(feedItems)
    val latestOnVisibleFeedItemsChanged by rememberUpdatedState(onVisibleFeedItemsChanged)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .sortedBy { it.offset }
                .mapNotNull { visibleItem ->
                    val feedItem = latestFeedItems.getOrNull(visibleItem.index) ?: return@mapNotNull null
                    VisibleFeedItem(
                        id = feedItem.id,
                        index = visibleItem.index,
                    )
                }
        }
            .distinctUntilChanged()
            .collect { visibleItems ->
                latestOnVisibleFeedItemsChanged(visibleItems)
            }
    }
}

@Composable
private fun ObserveVisibleGridFeedItems(
    feedItems: ImmutableList<FeedItem>,
    gridState: LazyStaggeredGridState,
    onVisibleFeedItemsChanged: (List<VisibleFeedItem>) -> Unit,
) {
    val latestFeedItems by rememberUpdatedState(feedItems)
    val latestOnVisibleFeedItemsChanged by rememberUpdatedState(onVisibleFeedItemsChanged)
    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo
                .sortedWith(compareBy({ it.offset.y }, { it.offset.x }))
                .mapNotNull { visibleItem ->
                    val feedItem = latestFeedItems.getOrNull(visibleItem.index) ?: return@mapNotNull null
                    VisibleFeedItem(
                        id = feedItem.id,
                        index = visibleItem.index,
                    )
                }
        }
            .distinctUntilChanged()
            .collect { visibleItems ->
                latestOnVisibleFeedItemsChanged(visibleItems)
            }
    }
}

@Composable
private fun MarkAllReadButton(
    showNextFeedButton: Boolean,
    nextFeedState: NextFeedDisplayState,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        TextButton(
            modifier = Modifier
                .padding(top = Spacing.small)
                .padding(
                    bottom = if (showNextFeedButton && nextFeedState is NextFeedDisplayEnabledState) {
                        Spacing.small
                    } else {
                        Spacing.medium
                    },
                )
                .align(Alignment.Center),
            onClick = onClick,
        ) {
            Text(LocalFeedFlowStrings.current.markAllReadButton)
        }
    }
}

@Composable
internal fun NavigateNextButton(
    title: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxWidth(),
    ) {
        TextButton(
            modifier = Modifier
                .padding(bottom = Spacing.medium)
                .align(Alignment.Center),
            onClick = onClick,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xsmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun FeedItemContainer(
    feedLayout: FeedLayout,
    modifier: Modifier = Modifier,
    isGridCell: Boolean = false,
    content: @Composable (Modifier) -> Unit,
) {
    when (feedLayout.normalizeForFeedList()) {
        FeedLayout.LIST -> content(modifier)
        FeedLayout.CARD -> {
            FeedItemCard(
                modifier = if (isGridCell) {
                    modifier.fillMaxWidth()
                } else {
                    modifier.padding(Spacing.small)
                },
            ) {
                content(Modifier)
            }
        }
        FeedLayout.BIG_IMAGE -> {
            FeedItemCard(
                modifier = if (isGridCell) {
                    modifier.fillMaxWidth()
                } else {
                    modifier.padding(
                        horizontal = Spacing.regular,
                        vertical = Spacing.small,
                    )
                },
            ) {
                content(Modifier)
            }
        }
        FeedLayout.GRID -> content(modifier)
    }
}

private fun FeedLayout.normalizeForFeedList(): FeedLayout =
    if (this == FeedLayout.GRID) FeedLayout.BIG_IMAGE else this

private fun FeedLayout.supportsGridArrangement(): Boolean =
    this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE

private fun FeedLayout.usesCardBackground(): Boolean =
    this == FeedLayout.CARD || this == FeedLayout.BIG_IMAGE || this == FeedLayout.GRID

private fun FeedLayout.gridMinCellWidth(): Dp = GridMinCellWidth

private fun FeedLayout.gridMinContentWidth(): Dp =
    gridMinCellWidth() * 2 + Spacing.regular

private fun PaddingValues.withHorizontal(padding: Dp): PaddingValues = PaddingValues(
    start = padding,
    top = calculateTopPadding(),
    end = padding,
    bottom = calculateBottomPadding(),
)

@Composable
private fun FeedItemCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        content()
    }
}

private fun SwipeActionType.toSwipeAction(
    feedItem: FeedItem,
    swipeBackgroundColor: Color,
    onOpenInBrowser: (FeedItemUrlInfo) -> Unit,
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
                    contentDescription = if (feedItem.isRead) {
                        LocalFeedFlowStrings.current.menuMarkAsUnread
                    } else {
                        LocalFeedFlowStrings.current.menuMarkAsRead
                    },
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
                    contentDescription = if (feedItem.isBookmarked) {
                        LocalFeedFlowStrings.current.menuRemoveFromBookmark
                    } else {
                        LocalFeedFlowStrings.current.menuAddToBookmark
                    },
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

        OPEN_IN_BROWSER -> SwipeAction(
            icon = {
                Icon(
                    modifier = Modifier.padding(Spacing.regular),
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = LocalFeedFlowStrings.current.readerModeBrowserButtonContentDescription,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            background = swipeBackgroundColor,
            onSwipe = {
                onOpenInBrowser(feedItem.toSwipeActionUrlInfo())
                if (!feedItem.isRead) {
                    onReadStatusClick(
                        FeedItemId(feedItem.id),
                        true,
                    )
                }
            },
        )

        NONE -> null
    }
}

private fun FeedItem.toSwipeActionUrlInfo(): FeedItemUrlInfo =
    FeedItemUrlInfo(
        id = id,
        url = url,
        title = title,
        isBookmarked = isBookmarked,
        linkOpeningPreference = LinkOpeningPreference.PREFERRED_BROWSER,
        commentsUrl = commentsUrl,
        imageUrl = imageUrl,
    )

@Preview
@Composable
internal fun FeedListPreview() {
    PreviewHelper {
        PreviewColumn {
            FeedList(
                feedItems = feedItemsForPreview,
                nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.LIST,
                currentFeedFilter = FeedFilter.Timeline,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                onVisibleFeedItemsChanged = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onOpenInBrowser = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onCommentClick = {},
                markAllAsRead = {},
                onShareClick = {},
                onOpenFeedSettings = {},
                onOpenFeedWebsite = {},
                onNavigateNext = {},
            )

            FeedList(
                feedItems = feedItemsForPreview,
                nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                feedFontSize = FeedFontSizes(),
                feedLayout = FeedLayout.CARD,
                currentFeedFilter = FeedFilter.Timeline,
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share with comments",
                swipeActions = SwipeActions(
                    leftSwipeAction = TOGGLE_READ_STATUS,
                    rightSwipeAction = TOGGLE_BOOKMARK_STATUS,
                ),
                onVisibleFeedItemsChanged = {},
                requestMoreItems = {},
                onFeedItemClick = {},
                onOpenInBrowser = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onCommentClick = {},
                markAllAsRead = {},
                onShareClick = {},
                onOpenFeedSettings = {},
                onOpenFeedWebsite = {},
                onNavigateNext = {},
            )
        }
    }
}
