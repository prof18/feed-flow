package com.prof18.feedflow.android.home.bywindowsize

import FeedFlowTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.android.home.components.HomeScaffold
import com.prof18.feedflow.android.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun CompactHomeView(
    feedItems: ImmutableList<FeedItem>,
    navDrawerState: NavDrawerState,
    unReadCount: Long,
    snackbarHostState: SnackbarHostState,
    feedUpdateStatus: FeedUpdateStatus,
    currentFeedFilter: FeedFilter,
    onAddFeedClick: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onClearOldArticlesClicked: () -> Unit,
    onDeleteDatabaseClick: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    refreshData: () -> Unit,
    requestNewData: () -> Unit,
    forceRefreshData: () -> Unit,
    markAsReadOnScroll: (Int) -> Unit,
    markAsRead: (FeedItemId) -> Unit,
    markAllRead: () -> Unit,
    openUrl: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = feedUpdateStatus.isLoading(),
        onRefresh = refreshData,
    )

    if (feedItems.isEmpty() && navDrawerState.isEmpty()) {
        HomeScaffold(
            unReadCount = unReadCount,
            onSettingsButtonClicked = onSettingsButtonClicked,
            scope = scope,
            listState = listState,
            snackbarHostState = snackbarHostState,
            loadingState = feedUpdateStatus,
            feedState = feedItems,
            pullRefreshState = pullRefreshState,
            showDrawerMenu = false,
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
            onAddFeedClick = onAddFeedClick,
            onClearOldArticlesClicked = onClearOldArticlesClicked,
            refreshData = refreshData,
            requestNewData = requestNewData,
            forceRefreshData = forceRefreshData,
            onDeleteDatabaseClick = onDeleteDatabaseClick,
            markAsReadOnScroll = markAsReadOnScroll,
            markAsRead = markAsRead,
            markAllRead = markAllRead,
            openUrl = openUrl,
        )
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet {
                    Drawer(
                        navDrawerState = navDrawerState,
                        currentFeedFilter = currentFeedFilter,
                        feedSourceImage = { imageUrl ->
                            FeedSourceLogoImage(imageUrl = imageUrl)
                        },
                        onFeedFilterSelected = { feedFilter ->
                            onFeedFilterSelected(feedFilter)
                            scope.launch {
                                drawerState.close()
                                listState.animateScrollToItem(0)
                            }
                        },
                    )
                }
            },
            drawerState = drawerState,
        ) {
            HomeScaffold(
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                scope = scope,
                listState = listState,
                snackbarHostState = snackbarHostState,
                loadingState = feedUpdateStatus,
                feedState = feedItems,
                pullRefreshState = pullRefreshState,
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
                onAddFeedClick = onAddFeedClick,
                refreshData = refreshData,
                requestNewData = requestNewData,
                forceRefreshData = forceRefreshData,
                onDeleteDatabaseClick = onDeleteDatabaseClick,
                markAsReadOnScroll = markAsReadOnScroll,
                markAsRead = markAsRead,
                markAllRead = markAllRead,
                onClearOldArticlesClicked = onClearOldArticlesClicked,
                openUrl = openUrl,
            )
        }
    }
}

@FeedFlowPhonePreview
@Composable
private fun CompactHomeViewPreview() {
    FeedFlowTheme {
        CompactHomeView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            snackbarHostState = SnackbarHostState(),
            feedUpdateStatus = inProgressFeedUpdateStatus,
            currentFeedFilter = FeedFilter.Timeline,
            onAddFeedClick = {},
            onSettingsButtonClicked = {},
            onClearOldArticlesClicked = {},
            onDeleteDatabaseClick = {},
            onFeedFilterSelected = {},
            refreshData = {},
            requestNewData = {},
            forceRefreshData = {},
            markAsReadOnScroll = {},
            markAsRead = {},
            markAllRead = {},
            openUrl = { },
        )
    }
}
