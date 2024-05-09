package com.prof18.feedflow.desktop.home.bywindowsize

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.home.components.HomeScreenContent
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun CompactView(
    feedItems: ImmutableList<FeedItem>,
    navDrawerState: NavDrawerState,
    unReadCount: Long,
    currentFeedFilter: FeedFilter,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    lazyListState: LazyListState,
    onAddFeedClick: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    refreshData: () -> Unit,
    requestNewData: () -> Unit,
    markAsReadOnScroll: (Int) -> Unit,
    markAsRead: (FeedItemId) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
    onBackToTimelineClick: () -> Unit,
    onSearchClick: () -> Unit,
    openUrl: (FeedItemUrlInfo) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isDrawerHidden = currentFeedFilter is FeedFilter.Timeline && feedItems.isEmpty() && navDrawerState.isEmpty()
    if (isDrawerHidden) {
        HomeScreenContent(
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedItems,
            listState = lazyListState,
            unReadCount = unReadCount,
            showDrawerMenu = true,
            currentFeedFilter = currentFeedFilter,
            onDrawerMenuClick = {
                scope.launch {
                    if (drawerState.isOpen) {
                        drawerState.close()
                    } else {
                        drawerState.open()
                    }
                }
            },
            onRefresh = refreshData,
            updateReadStatus = markAsReadOnScroll,
            onFeedItemClick = { feedInfo ->
                openUrl(feedInfo)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onCommentClick = { feedInfo ->
                openUrl(feedInfo)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onAddFeedClick = {
                onAddFeedClick()
            },
            requestMoreItems = requestNewData,
            onBookmarkClick = onBookmarkClick,
            onReadStatusClick = onReadStatusClick,
            onBackToTimelineClick = onBackToTimelineClick,
            onSearchClick = onSearchClick,
        )
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Drawer(
                        navDrawerState = navDrawerState,
                        currentFeedFilter = currentFeedFilter,
                        onAddFeedClicked = onAddFeedClick,
                        onFeedFilterSelected = { feedFilter ->
                            onFeedFilterSelected(feedFilter)
                            scope.launch {
                                drawerState.close()
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                    )
                }
            },
            drawerState = drawerState,
        ) {
            HomeScreenContent(
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedState = feedItems,
                listState = lazyListState,
                unReadCount = unReadCount,
                showDrawerMenu = true,
                currentFeedFilter = currentFeedFilter,
                onDrawerMenuClick = {
                    scope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                        } else {
                            drawerState.open()
                        }
                    }
                },
                onRefresh = refreshData,
                updateReadStatus = markAsReadOnScroll,
                onFeedItemClick = { feedInfo ->
                    openInBrowser(feedInfo.url)
                    markAsRead(FeedItemId(feedInfo.id))
                },
                onCommentClick = { feedInfo ->
                    openInBrowser(feedInfo.url)
                    markAsRead(FeedItemId(feedInfo.id))
                },
                onAddFeedClick = {
                    onAddFeedClick()
                },
                requestMoreItems = requestNewData,
                onBookmarkClick = onBookmarkClick,
                onReadStatusClick = onReadStatusClick,
                onBackToTimelineClick = onBackToTimelineClick,
                onSearchClick = onSearchClick,
            )
        }
    }
}

@Preview
@Composable
private fun CompactViewPreview() {
    FeedFlowTheme {
        CompactView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            currentFeedFilter = FeedFilter.Timeline,
            paddingValues = PaddingValues(),
            loadingState = inProgressFeedUpdateStatus,
            lazyListState = LazyListState(),
            onAddFeedClick = {},
            onFeedFilterSelected = {},
            refreshData = {},
            requestNewData = {},
            markAsReadOnScroll = {},
            markAsRead = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onBackToTimelineClick = {},
            onSearchClick = {},
            openUrl = {},
        )
    }
}
