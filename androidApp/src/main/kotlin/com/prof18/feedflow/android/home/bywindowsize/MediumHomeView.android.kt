package com.prof18.feedflow.android.home.bywindowsize

import FeedFlowTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.home.components.HomeScaffold
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.preview.PreviewFoldable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun MediumHomeView(
    feedItems: ImmutableList<FeedItem>,
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    unReadCount: Long,
    snackbarHostState: SnackbarHostState,
    feedUpdateStatus: FeedUpdateStatus,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
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
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isDrawerMenuFullVisible by remember {
        mutableStateOf(true)
    }

    val isDrawerHidden = currentFeedFilter is FeedFilter.Timeline && feedItems.isEmpty() && navDrawerState.isEmpty()
    Row {
        AnimatedVisibility(
            modifier = Modifier
                .weight(1f),
            visible = isDrawerMenuFullVisible && !isDrawerHidden,
        ) {
            Scaffold { paddingValues ->
                Drawer(
                    modifier = Modifier
                        .padding(paddingValues),
                    navDrawerState = navDrawerState,
                    currentFeedFilter = currentFeedFilter,
                    onAddFeedClicked = onAddFeedClick,
                    onFeedFilterSelected = { feedFilter ->
                        onFeedFilterSelected(feedFilter)
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    onEditFeedClick = onEditFeedClick,
                    onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                )
            }
        }

        HomeScaffold(
            modifier = Modifier
                .weight(2f),
            unReadCount = unReadCount,
            onSettingsButtonClicked = onSettingsButtonClicked,
            scope = scope,
            listState = listState,
            snackbarHostState = snackbarHostState,
            loadingState = feedUpdateStatus,
            feedState = feedItems,
            showDrawerMenu = feedItems.isNotEmpty(),
            isDrawerMenuOpen = isDrawerMenuFullVisible,
            currentFeedFilter = currentFeedFilter,
            onDrawerMenuClick = {
                isDrawerMenuFullVisible = !isDrawerMenuFullVisible
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

@PreviewFoldable
@Composable
private fun MediumHomeViewPreview() {
    FeedFlowTheme {
        MediumHomeView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            snackbarHostState = SnackbarHostState(),
            feedUpdateStatus = inProgressFeedUpdateStatus,
            currentFeedFilter = FeedFilter.Timeline,
            onSettingsButtonClicked = {},
            onAddFeedClick = {},
            onClearOldArticlesClicked = {},
            refreshData = {},
            requestNewData = {},
            forceRefreshData = {},
            onDeleteDatabaseClick = {},
            markAsReadOnScroll = {},
            markAsRead = {},
            markAllRead = {},
            onFeedFilterSelected = {},
            openUrl = {},
            updateReadStatus = { _, _ -> },
            updateBookmarkStatus = { _, _ -> },
            onBackToTimelineClick = {},
            onSearchClick = {},
            onEditFeedClick = { _ -> },
            onDeleteFeedSourceClick = { _ -> },
        )
    }
}
