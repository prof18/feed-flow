package com.prof18.feedflow.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.presentation.model.HomeViewMenuState
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

private val listTopContentPadding = 4.dp
private val floatingToolbarHeight = 64.dp
private val scrimFeather = 24.dp

@Composable
fun AndroidHomeScreenContent(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    viewMenuState: HomeViewMenuState,
    onFeedOrderChange: (FeedOrder) -> Unit,
    onShowReadArticlesTimelineChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
    onNavigateToNextFeed: () -> Unit = { },
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
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            floatingToolbarHeight +
            FloatingToolbarDefaults.ScreenOffset

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = innerPadding.calculateLeftPadding(layoutDir))
                .padding(end = innerPadding.calculateRightPadding(layoutDir)),
        ) {
            when {
                displayState.feedUpdateStatus is NoFeedSourcesStatus -> NoFeedsSourceView(
                    modifier = Modifier.padding(top = topInset),
                    onAddFeedClick = onEmptyStateClick ?: feedManagementActions.onAddFeedClick,
                )

                !displayState.feedUpdateStatus.isLoading() && displayState.feedItems.isEmpty() -> EmptyFeedView(
                    modifier = Modifier.padding(top = topInset),
                    currentFeedFilter = displayState.currentFeedFilter,
                    onReloadClick = onRefresh,
                    onBackToTimelineClick = feedListActions.onBackToTimelineClick,
                    onOpenDrawerClick = onDrawerMenuClick,
                    isDrawerVisible = showDrawerMenu,
                )

                else -> {
                    val pullToRefreshState = rememberPullToRefreshState()
                    val isRefreshing = displayState.feedUpdateStatus.isLoading()
                    PullToRefreshBox(
                        modifier = Modifier.fillMaxSize(),
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = topInset),
                                state = pullToRefreshState,
                                isRefreshing = isRefreshing,
                            )
                        },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            FeedLoader(
                                loadingState = displayState.feedUpdateStatus,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = topInset + Spacing.small),
                            )

                            val listTopPadding = if (isRefreshing) {
                                listTopContentPadding
                            } else {
                                topInset + listTopContentPadding
                            }

                            if (displayState.feedItems.isEmpty() && isRefreshing) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                ) {
                                    CircularProgressIndicator()
                                }
                            } else {
                                FeedList(
                                    modifier = Modifier.weight(1f),
                                    feedItems = displayState.feedItems,
                                    listState = listState,
                                    contentPadding = PaddingValues(top = listTopPadding),
                                    feedFontSize = displayState.feedFontSizes,
                                    nextFeedState = displayState.nextFeedDisplayState,
                                    shareCommentsMenuLabel = shareBehavior.shareCommentsTitle,
                                    shareMenuLabel = shareBehavior.shareLinkTitle,
                                    currentFeedFilter = displayState.currentFeedFilter,
                                    swipeActions = displayState.swipeActions,
                                    requestMoreItems = feedListActions.requestNewData,
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
                                    updateReadStatus = feedListActions.markAsReadOnScroll,
                                    markAllAsRead = feedListActions.markAllRead,
                                    onShareClick = shareBehavior.onShareClick,
                                    onOpenFeedSettings = feedManagementActions.onEditFeedClick,
                                    onOpenFeedWebsite = feedManagementActions.onOpenWebsite,
                                    feedLayout = displayState.feedLayout,
                                    onMarkAllAboveAsRead = feedListActions.markAllAboveAsRead,
                                    onMarkAllBelowAsRead = feedListActions.markAllBelowAsRead,
                                    onNavigateNext = { onNavigateToNextFeed() },
                                    feedItemDisplaySettings = displayState.feedItemDisplaySettings,
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(topInset + scrimFeather)
                    .zIndex(zIndex = 0.5f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            HomeFloatingToolbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f),
                displayState = displayState,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                onSearchClick = onSearchClick,
                onMarkAllReadClicked = feedListActions.markAllRead,
                onClearOldArticlesClicked = feedListActions.onClearOldArticlesClicked,
                onForceRefreshClick = {
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                        feedListActions.forceRefreshData()
                    }
                },
                onEditFeedClick = feedManagementActions.onEditFeedClick,
                onClick = {
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
                        onRefresh()
                    }
                },
                onBackupClick = onBackupClick,
                viewMenuState = viewMenuState,
                onFeedOrderChange = onFeedOrderChange,
                onShowReadArticlesTimelineChange = onShowReadArticlesTimelineChange,
            )
        }
    }
}
