package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
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
    showDrawerMenu: Boolean = true,
    isDrawerOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val reduceMotionEnabled = LocalReduceMotion.current

    @Suppress("MagicNumber")
    val showScrollToTopButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DesktopHomeAppBar(
                currentFeedFilter = displayState.currentFeedFilter,
                unReadCount = displayState.unReadCount,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                onSearchClick = onSearchClick,
                onClick = {
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
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
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDir = LocalLayoutDirection.current

        Column(
            modifier = Modifier
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = listPaneMaxContentWidth)
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                            ) {
                                Column {
                                    FeedLoader(loadingState = displayState.feedUpdateStatus)

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
                                        feedFontSize = displayState.feedFontSizes,
                                        shareCommentsMenuLabel = shareBehavior.shareCommentsTitle,
                                        shareMenuLabel = shareBehavior.shareLinkTitle,
                                        currentFeedFilter = displayState.currentFeedFilter,
                                        swipeActions = displayState.swipeActions,
                                        requestMoreItems = feedListActions.requestNewData,
                                        onFeedItemClick = { feedInfo ->
                                            feedListActions.openUrl(feedInfo)
                                            feedListActions.markAsRead(FeedItemId(feedInfo.id))
                                        },
                                        onBookmarkClick = feedListActions.updateBookmarkStatus,
                                        onReadStatusClick = feedListActions.updateReadStatus,
                                        onCommentClick = { feedInfo ->
                                            feedListActions.openUrl(feedInfo)
                                            feedListActions.markAsRead(FeedItemId(feedInfo.id))
                                        },
                                        updateReadStatus = feedListActions.markAsReadOnScroll,
                                        markAllAsRead = feedListActions.markAllRead,
                                        onShareClick = shareBehavior.onShareClick,
                                        onOpenFeedSettings = feedManagementActions.onEditFeedClick,
                                        onOpenFeedWebsite = feedManagementActions.onOpenWebsite,
                                        feedLayout = displayState.feedLayout,
                                        onMarkAllAboveAsRead = feedListActions.markAllAboveAsRead,
                                        onMarkAllBelowAsRead = feedListActions.markAllBelowAsRead,
                                    )
                                }
                            }
                        }

                        VerticalScrollbar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(6.dp),
                            adapter = rememberScrollbarAdapter(scrollState = listState),
                        )

                        TopToolbarContentFade(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = listPaneTopContentFadeHeight,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}
