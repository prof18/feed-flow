package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.list.FeedList
import kotlinx.coroutines.launch

@Composable
fun HomeScreenContent(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onSearchClick: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    showDrawerMenu: Boolean = false,
    isDrawerOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    toolbarContent: @Composable () -> Unit = {},
    feedContentWrapper: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
    showDropdownMenu: Boolean = false,
) {
    val scope = rememberCoroutineScope()

    @Suppress("MagicNumber")
    val showScrollToTopButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            HomeAppBar(
                currentFeedFilter = displayState.currentFeedFilter,
                unReadCount = displayState.unReadCount,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                onSearchClick = onSearchClick,
                showDropdownMenu = showDropdownMenu,
                onMarkAllReadClicked = feedListActions.markAllRead,
                onClearOldArticlesClicked = feedListActions.onClearOldArticlesClicked,
                onSettingsButtonClicked = onSettingsButtonClicked,
                onForceRefreshClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        feedListActions.forceRefreshData()
                    }
                },
                onEditFeedClick = feedManagementActions.onEditFeedClick,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onDoubleClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                        onRefresh()
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ScrollToTopButton(
                visible = showScrollToTopButton,
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
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
            toolbarContent()

            when {
                displayState.feedUpdateStatus is NoFeedSourcesStatus -> NoFeedsSourceView(
                    onAddFeedClick = feedManagementActions.onAddFeedClick,
                )

                !displayState.feedUpdateStatus.isLoading() && displayState.feedItems.isEmpty() -> EmptyFeedView(
                    currentFeedFilter = displayState.currentFeedFilter,
                    onReloadClick = onRefresh,
                    onBackToTimelineClick = feedListActions.onBackToTimelineClick,
                    onOpenDrawerClick = onDrawerMenuClick,
                    isDrawerVisible = showDrawerMenu,
                )

                else -> feedContentWrapper {
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
                            feedLayout = displayState.feedLayout,
                        )
                    }
                }
            }
        }
    }
}
