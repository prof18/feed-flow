package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedItemUrlTitle
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.VisibleFeedItem
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.foundation.lazy.staggeredgrid.items as gridItems

@Suppress("LongMethod")
@Composable
internal fun DesktopArticleList(
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
    showFooter: Boolean = true,
    selectedArticleId: String? = null,
    markAllAsReadEnabled: Boolean = true,
) {
    val ids = remember(feedItems) { feedItems.map { it.id }.toImmutableList() }
    val normalizedLayout = if (feedLayout == FeedLayout.GRID) FeedLayout.BIG_IMAGE else feedLayout
    val latestItems by rememberUpdatedState(feedItems)
    val latestVisibility by rememberUpdatedState(onVisibleFeedItemsChanged)
    val latestRequestMore by rememberUpdatedState(requestMoreItems)
    val latestIds by rememberUpdatedState(ids)
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var focusedId by remember { mutableStateOf<String?>(null) }
    val latestGridChanged by rememberUpdatedState(onGridArrangementChanged)

    SideEffect(ids) {
        focusRequesters.keys.retainAll(ids.toSet())
        if (focusedId !in ids) focusedId = ids.firstOrNull()
    }

    val listBackground = if (normalizedLayout == FeedLayout.LIST) {
        JewelTheme.globalColors.panelBackground
    } else {
        articleCardContainerColor()
    }
    BoxWithConstraints(modifier = modifier.background(listBackground)) {
        val useGrid = isGridLayoutEnabled && isGridLayoutAllowed &&
            normalizedLayout != FeedLayout.LIST && maxWidth >= GridMinimumWidth
        SideEffect(useGrid) { latestGridChanged(useGrid) }

        val visibleKeys = { visibleArticleKeys(useGrid, listState, gridState, latestIds) }
        LaunchedEffect(listState, gridState, useGrid, currentFeedFilter) {
            snapshotFlow { visibleArticles(latestIds, visibleKeys()) }
                .distinctUntilChanged()
                .collect { latestVisibility(it) }
        }
        LaunchedEffect(listState, gridState, useGrid, currentFeedFilter) {
            snapshotFlow { articlePageRequest(latestIds, visibleKeys()) }
                .distinctUntilChanged()
                .collect { if (it != null) latestRequestMore() }
        }

        val keyboardModifier = rememberArticleKeyboardModifier(
            ids = ids,
            focusedId = focusedId,
            onFocusedIdChange = { focusedId = it },
            useGrid = useGrid,
            visibleKeys = visibleKeys,
            latestItems = latestItems,
            requestFocus = { focusRequesters[it]?.requestFocus() },
            listState = listState,
            gridState = gridState,
        )

        @Composable
        fun Article(item: FeedItem) {
            val requester = focusRequesters.getOrPut(item.id) { FocusRequester() }
            ArticleSwipeRow(
                item = item,
                swipeActions = swipeActions,
                enableSwipe = !useGrid,
                onOpenInBrowser = onOpenInBrowser,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
            ) {
                ArticleRow(
                    feedItem = item,
                    feedFontSize = feedFontSize,
                    shareMenuLabel = shareMenuLabel,
                    shareCommentsMenuLabel = shareCommentsMenuLabel,
                    feedLayout = normalizedLayout,
                    currentFeedFilter = currentFeedFilter,
                    feedItemDisplaySettings = feedItemDisplaySettings,
                    onFeedItemClick = onFeedItemClick,
                    onOpenInBrowser = onOpenInBrowser,
                    onBookmarkClick = onBookmarkClick,
                    onReadStatusClick = onReadStatusClick,
                    onCommentClick = onCommentClick,
                    onOpenFeedSettings = onOpenFeedSettings,
                    onOpenFeedWebsite = onOpenFeedWebsite,
                    onShareClick = onShareClick,
                    onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                    onMarkAllBelowAsRead = onMarkAllBelowAsRead,
                    isGridCell = useGrid,
                    isSelected = item.id == selectedArticleId,
                    modifier = Modifier
                        .focusRequester(requester)
                        .onFocusChanged { if (it.isFocused) focusedId = item.id },
                )
            }
        }

        if (useGrid) {
            LazyVerticalStaggeredGrid(
                modifier = keyboardModifier.fillMaxSize(),
                columns = StaggeredGridCells.Adaptive(minSize = GridCellWidth),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding(),
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalItemSpacing = 16.dp,
            ) {
                // Unkeyed on purpose, like Android: the list keeps its position by index, so it stays at the
                // top when the filter changes or a refresh prepends newer articles.
                gridItems(feedItems, contentType = { normalizedLayout }) { Article(it) }
                if (showFooter) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        ArticleFooter(
                            currentFeedFilter,
                            showNextFeedButton,
                            nextFeedState,
                            markAllAsRead,
                            onNavigateNext,
                            markAllAsReadEnabled,
                        )
                    }
                }
            }
        } else {
            LazyColumn(modifier = keyboardModifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
                items(feedItems, contentType = { normalizedLayout }) { Article(it) }
                if (showFooter) {
                    item {
                        ArticleFooter(
                            currentFeedFilter,
                            showNextFeedButton,
                            nextFeedState,
                            markAllAsRead,
                            onNavigateNext,
                            markAllAsReadEnabled,
                        )
                    }
                }
            }
        }
    }
}

private fun visibleArticleKeys(
    useGrid: Boolean,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    ids: List<String>,
): List<Any> {
    val indexes = if (useGrid) {
        gridState.layoutInfo.visibleItemsInfo.sortedWith(compareBy({ it.offset.y }, { it.offset.x })).map { it.index }
    } else {
        listState.layoutInfo.visibleItemsInfo.sortedBy { it.offset }.map { it.index }
    }
    return indexes.map { index -> ids.getOrNull(index)?.let(::ArticleKey) ?: ArticleFooterKey }
}

@Composable
private fun rememberArticleKeyboardModifier(
    ids: ImmutableList<String>,
    focusedId: String?,
    onFocusedIdChange: (String?) -> Unit,
    useGrid: Boolean,
    visibleKeys: () -> List<Any>,
    latestItems: ImmutableList<FeedItem>,
    requestFocus: (String) -> Unit,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
): Modifier {
    val scope = rememberCoroutineScope()
    val currentItems by rememberUpdatedState(latestItems)
    return Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val delta = when (event.key) {
            Key.DirectionDown -> 1
            Key.DirectionUp -> -1
            else -> return@onPreviewKeyEvent false
        }
        val nextId = moveArticleFocus(ids, focusedId, delta) ?: return@onPreviewKeyEvent false
        onFocusedIdChange(nextId)
        scope.launch {
            if (ArticleKey(nextId) !in visibleKeys()) {
                val index = currentItems.indexOfFirst { it.id == nextId }
                if (index >= 0) {
                    if (useGrid) gridState.scrollToItem(index) else listState.scrollToItem(index)
                }
            }
            withFrameNanos { }
            requestFocus(nextId)
        }
        true
    }
}

@Composable
private fun ArticleFooter(
    currentFeedFilter: FeedFilter,
    showNextFeedButton: Boolean,
    nextFeedState: NextFeedDisplayState,
    markAllAsRead: () -> Unit,
    onNavigateNext: () -> Unit,
    markAllAsReadEnabled: Boolean,
) {
    if (currentFeedFilter is FeedFilter.Read && !showNextFeedButton) return
    val wrappingStyle = rememberWrappingOutlinedButtonStyle()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (currentFeedFilter !is FeedFilter.Read) {
            OutlinedButton(
                onClick = markAllAsRead,
                enabled = markAllAsReadEnabled,
                style = wrappingStyle,
                modifier = Modifier.heightIn(min = 36.dp),
            ) {
                Text(LocalFeedFlowStrings.current.markAllReadButton)
            }
        }
        if (showNextFeedButton && nextFeedState is NextFeedDisplayState.NextFeedDisplayEnabledState) {
            OutlinedButton(
                onClick = onNavigateNext,
                style = wrappingStyle,
                modifier = Modifier.heightIn(min = 36.dp),
            ) { Text(nextFeedState.title) }
        }
    }
}

private val GridCellWidth = 280.dp
private val GridMinimumWidth = 576.dp
