package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.composeunstyled.rememberScrollbarState
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.shared.ui.components.TopToolbarContentFade
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.EmptyFeedView
import com.prof18.feedflow.shared.ui.home.components.FeedLoader
import com.prof18.feedflow.shared.ui.home.components.NoFeedsSourceView
import com.prof18.feedflow.shared.ui.home.components.ScrollToTopButton
import com.prof18.feedflow.shared.ui.home.components.list.FeedList
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch

@Composable
fun DesktopHomeScreenContent(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    showDrawerMenu: Boolean = true,
    isGridLayoutAllowed: Boolean = true,
    isDrawerOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current
    var isGridArrangement by remember { mutableStateOf(false) }

    @Suppress("MagicNumber")
    val showScrollToTopButton by remember {
        derivedStateOf {
            shouldShowScrollToTopButton(
                isGridArrangement = isGridArrangement,
                listState = listState,
                gridState = gridState,
            )
        }
    }
    val contentContainerColor = displayState.feedLayout.contentContainerColor()

    Scaffold(
        modifier = modifier,
        containerColor = contentContainerColor,
        topBar = {
            DesktopHomeAppBar(
                currentFeedFilter = displayState.currentFeedFilter,
                unReadCount = displayState.unReadCount,
                isUnreadCountHidden = displayState.isUnreadCountHidden,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                onSearchClick = onSearchClick,
                onClick = {
                    scope.launch {
                        scrollFeedToTop(
                            isGridArrangement = isGridArrangement,
                            listState = listState,
                            gridState = gridState,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        scrollFeedToTop(
                            isGridArrangement = isGridArrangement,
                            listState = listState,
                            gridState = gridState,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                        feedListActions.refreshData()
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            data.dismiss()
                        }
                        true
                    },
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    content = { Snackbar(data) },
                )
            }
        },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showScrollToTopButton,
                onClick = {
                    scope.launch {
                        scrollFeedToTop(
                            isGridArrangement = isGridArrangement,
                            listState = listState,
                            gridState = gridState,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { innerPadding ->
        val layoutDir = LocalLayoutDirection.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(contentContainerColor)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(start = innerPadding.calculateLeftPadding(layoutDir))
                .padding(end = innerPadding.calculateRightPadding(layoutDir)),
        ) {
            when {
                displayState.feedUpdateStatus is NoFeedSourcesStatus -> NoFeedsSourceView(
                    onAddFeedClick = onEmptyStateClick ?: feedManagementActions.onAddFeedClick,
                )

                !displayState.feedUpdateStatus.isLoading() && displayState.feedItems.isEmpty() -> EmptyFeedView(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onReloadClick = feedListActions.refreshData,
                    onBackToTimelineClick = feedListActions.onBackToTimelineClick,
                    onOpenDrawerClick = onDrawerMenuClick,
                    isDrawerVisible = showDrawerMenu,
                )

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val showListScrollbar = !isGridArrangement
                        val feedListWidthModifier = displayState.feedLayout.feedListWidthModifier(
                            isGridLayoutEnabled = displayState.isGridLayoutEnabled,
                            isGridLayoutAllowed = isGridLayoutAllowed,
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = feedListWidthModifier.fillMaxHeight(),
                            ) {
                                Column {
                                    FeedLoader(
                                        loadingState = displayState.feedUpdateStatus,
                                        modifier = Modifier.padding(
                                            top = Spacing.small,
                                            bottom = Spacing.small,
                                        ),
                                    )

                                    if (displayState.feedItems.isEmpty() && displayState.feedUpdateStatus.isLoading()) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize(),
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }

                                    FeedList(
                                        modifier = Modifier,
                                        feedItems = displayState.feedItems,
                                        listState = listState,
                                        gridState = gridState,
                                        contentPadding = PaddingValues(
                                            top = listPaneTopContentPadding,
                                            end = displayState.feedLayout.endContentInset(),
                                        ),
                                        feedFontSize = displayState.feedFontSizes,
                                        nextFeedState = displayState.nextFeedDisplayState,
                                        shareCommentsMenuLabel = shareBehavior.shareCommentsTitle,
                                        shareMenuLabel = shareBehavior.shareLinkTitle,
                                        currentFeedFilter = displayState.currentFeedFilter,
                                        swipeActions = displayState.swipeActions,
                                        requestMoreItems = feedListActions.requestNewData,
                                        onGridArrangementChanged = { isGridArrangement = it },
                                        onFeedItemClick = { feedInfo ->
                                            feedListActions.openUrl(feedInfo)
                                            feedListActions.markAsRead(FeedItemId(feedInfo.id))
                                        },
                                        onOpenInBrowser = feedListActions.openInBrowser,
                                        onBookmarkClick = feedListActions.updateBookmarkStatus,
                                        onReadStatusClick = feedListActions.updateReadStatus,
                                        onCommentClick = { feedInfo ->
                                            feedListActions.openUrl(feedInfo)
                                            feedListActions.markAsRead(FeedItemId(feedInfo.id))
                                        },
                                        onVisibleFeedItemsChanged = feedListActions.onVisibleFeedItemsChanged,
                                        markAllAsRead = feedListActions.markAllRead,
                                        onShareClick = shareBehavior.onShareClick,
                                        onOpenFeedSettings = feedManagementActions.onEditFeedClick,
                                        onOpenFeedWebsite = feedManagementActions.onOpenWebsite,
                                        feedLayout = displayState.feedLayout,
                                        isGridLayoutEnabled = displayState.isGridLayoutEnabled,
                                        isGridLayoutAllowed = isGridLayoutAllowed,
                                        onMarkAllAboveAsRead = feedListActions.markAllAboveAsRead,
                                        onMarkAllBelowAsRead = feedListActions.markAllBelowAsRead,
                                        onNavigateNext = feedListActions.onNavigateNext,
                                        showNextFeedButton = true,
                                        feedItemDisplaySettings = displayState.feedItemDisplaySettings,
                                    )
                                }
                            }
                        }

                        if (showListScrollbar) {
                            val scrollbarState = rememberScrollbarState(listState)
                            FeedFlowVerticalScrollbar(scrollbarState)
                        }

                        TopToolbarContentFade(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = listPaneTopContentFadeHeight,
                            color = contentContainerColor,
                        )
                    }
                }
            }
        }
    }
}

private fun FeedLayout.endContentInset() =
    if (isCardStyleLayout()) Spacing.regular else 0.dp

private fun FeedLayout.isCardStyleLayout() = when (this) {
    FeedLayout.CARD,
    FeedLayout.BIG_IMAGE,
    FeedLayout.GRID,
    -> true
    FeedLayout.LIST -> false
}

@Composable
private fun FeedLayout.contentContainerColor(): Color =
    if (isCardStyleLayout()) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

private fun FeedLayout.feedListWidthModifier(
    isGridLayoutEnabled: Boolean,
    isGridLayoutAllowed: Boolean,
): Modifier =
    if (isGridLayoutEnabled && isGridLayoutAllowed && isCardStyleLayout()) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .widthIn(max = listPaneMaxContentWidth)
            .fillMaxWidth()
    }

private fun shouldShowScrollToTopButton(
    isGridArrangement: Boolean,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
): Boolean =
    if (isGridArrangement) {
        gridState.firstVisibleItemIndex > 1
    } else {
        listState.firstVisibleItemIndex > 1
    }

private suspend fun scrollFeedToTop(
    isGridArrangement: Boolean,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    reduceMotionEnabled: Boolean,
) {
    if (isGridArrangement) {
        gridState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
    } else {
        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
    }
}
