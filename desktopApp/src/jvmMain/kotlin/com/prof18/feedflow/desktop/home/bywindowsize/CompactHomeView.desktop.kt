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
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.desktop.home.components.HomeScreenContent
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.desktop.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import kotlinx.coroutines.launch

@Composable
internal fun CompactView(
    feedItems: List<FeedItem>,
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
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (feedItems.isEmpty() && navDrawerState.isEmpty()) {
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
            onFeedItemLongClick = { feedInfo ->
                openInBrowser(feedInfo.url)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onAddFeedClick = {
                onAddFeedClick()
            },
            requestMoreItems = requestNewData,
        )
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Drawer(
                        navDrawerState = navDrawerState,
                        currentFeedFilter = currentFeedFilter,
                        feedSourceImage = { imageUrl ->
                            FeedSourceLogoImage(
                                size = 24.dp,
                                imageUrl = imageUrl,
                            )
                        },
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
                onFeedItemLongClick = { feedInfo ->
                    openInBrowser(feedInfo.url)
                    markAsRead(FeedItemId(feedInfo.id))
                },
                onAddFeedClick = {
                    onAddFeedClick()
                },
                requestMoreItems = requestNewData,
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
        )
    }
}