package com.prof18.feedflow.android.home.bywindowsize

import FeedFlowTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.android.home.components.HomeScaffold
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun CompactHomeView(
    feedItems: ImmutableList<FeedItem>,
    navDrawerState: NavDrawerState,
    unReadCount: Long,
    snackbarHostState: SnackbarHostState,
    feedUpdateStatus: FeedUpdateStatus,
    feedFontSizes: FeedFontSizes,
    feedLayout: FeedLayout,
    currentFeedFilter: FeedFilter,
    swipeActions: SwipeActions,
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
    openUrl: (FeedItemUrlInfo) -> Unit,
    updateBookmarkStatus: (FeedItemId, Boolean) -> Unit,
    updateReadStatus: (FeedItemId, Boolean) -> Unit,
    onBackToTimelineClick: () -> Unit,
    onSearchClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

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
                            listState.animateScrollToItem(0)
                        }
                    },
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                    onEditFeedClick = onEditFeedClick,
                    onPinFeedClick = onPinFeedClick,
                    onEditCategoryClick = onEditCategoryClick,
                    onDeleteCategoryClick = onDeleteCategoryClick,
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
            feedFontSizes = feedFontSizes,
            feedLayout = feedLayout,
            showDrawerMenu = true,
            currentFeedFilter = currentFeedFilter,
            swipeActions = swipeActions,
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
            updateReadStatus = updateReadStatus,
            updateBookmarkStatus = updateBookmarkStatus,
            onBackToTimelineClick = onBackToTimelineClick,
            onSearchClick = onSearchClick,
            onEditFeedClick = onEditFeedClick,
        )
    }
}

@PreviewPhone
@Composable
private fun CompactHomeViewPreview() {
    FeedFlowTheme {
        CompactHomeView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            snackbarHostState = SnackbarHostState(),
            feedUpdateStatus = inProgressFeedUpdateStatus,
            feedFontSizes = FeedFontSizes(),
            feedLayout = FeedLayout.LIST,
            currentFeedFilter = FeedFilter.Timeline,
            swipeActions = SwipeActions(
                leftSwipeAction = SwipeActionType.NONE,
                rightSwipeAction = SwipeActionType.NONE,
            ),
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
            updateBookmarkStatus = { _, _ -> },
            updateReadStatus = { _, _ -> },
            onBackToTimelineClick = {},
            onSearchClick = {},
            onEditFeedClick = { },
            onDeleteFeedSourceClick = { },
            onPinFeedClick = { },
            onEditCategoryClick = { _, _ -> },
            onDeleteCategoryClick = { },
        )
    }
}
