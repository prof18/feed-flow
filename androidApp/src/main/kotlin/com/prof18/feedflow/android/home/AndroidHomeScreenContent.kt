package com.prof18.feedflow.android.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
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
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch

@Composable
fun AndroidHomeScreenContent(
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
    feedContentWrapper: @Composable (@Composable () -> Unit) -> Unit = { content -> content() },
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
        topBar = {
            AndroidHomeAppBar(
                currentFeedFilter = displayState.currentFeedFilter,
                unReadCount = displayState.unReadCount,
                showDrawerMenu = showDrawerMenu,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerMenuClick,
                onSearchClick = onSearchClick,
                onMarkAllReadClicked = feedListActions.markAllRead,
                onClearOldArticlesClicked = feedListActions.onClearOldArticlesClicked,
                onSettingsButtonClicked = onSettingsButtonClicked,
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
                isSyncUploadRequired = displayState.isSyncUploadRequired,
                onBackupClick = onBackupClick,
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
                    onReloadClick = onRefresh,
                    onBackToTimelineClick = feedListActions.onBackToTimelineClick,
                    onOpenDrawerClick = onDrawerMenuClick,
                    isDrawerVisible = showDrawerMenu,
                )

                else -> feedContentWrapper {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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

                        TopToolbarContentFade(
                            modifier = Modifier.align(Alignment.TopCenter),
                            height = 30.dp,
                            color = MaterialTheme.colorScheme.surface,
                        )
                    }
                }
            }
        }
    }
}
