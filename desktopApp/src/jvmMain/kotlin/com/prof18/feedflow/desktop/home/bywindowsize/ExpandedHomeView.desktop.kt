package com.prof18.feedflow.desktop.home.bywindowsize

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.NavDrawerState
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.home.components.HomeScreenContent
import com.prof18.feedflow.shared.domain.model.FeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.feedItemsForPreview
import com.prof18.feedflow.shared.presentation.preview.inProgressFeedUpdateStatus
import com.prof18.feedflow.shared.presentation.preview.navDrawerState
import com.prof18.feedflow.shared.ui.home.components.Drawer
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun ExpandedView(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedItems: ImmutableList<FeedItem>,
    feedFontSizes: FeedFontSizes,
    lazyListState: LazyListState,
    unReadCount: Long,
    swipeActions: SwipeActions,
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
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    markAllAsRead: () -> Unit,
    onEditCategoryClick: (CategoryId, CategoryName) -> Unit,
    onDeleteCategoryClick: (CategoryId) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.currentOrThrow
    val uriHandler = LocalUriHandler.current

    Row {
        Scaffold(
            modifier = Modifier
                .weight(1f),
        ) { paddingValues ->
            Drawer(
                modifier = Modifier
                    .padding(paddingValues),
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                onAddFeedClicked = onAddFeedClick,
                onFeedFilterSelected = { feedFilter ->
                    onFeedFilterSelected(feedFilter)
                    scope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                },
                onEditFeedClick = { feedSource ->
                    navigator.push(EditFeedScreen(feedSource))
                },
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onEditCategoryClick = onEditCategoryClick,
                onDeleteCategoryClick = onDeleteCategoryClick,
            )
        }

        HomeScreenContent(
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedItems,
            listState = lazyListState,
            feedFontSizes = feedFontSizes,
            unReadCount = unReadCount,
            currentFeedFilter = currentFeedFilter,
            swipeActions = swipeActions,
            modifier = Modifier
                .weight(2f),
            onRefresh = refreshData,
            updateReadStatus = markAsReadOnScroll,
            onFeedItemClick = { feedInfo ->
                openUrl(feedInfo)
                markAsRead(FeedItemId(feedInfo.id))
            },
            onCommentClick = { feedInfo ->
                uriHandler.openUri(feedInfo.url)
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
            markAllAsRead = markAllAsRead,
        )
    }
}

@Preview
@Composable
private fun ExpandedViewPreview() {
    FeedFlowTheme {
        ExpandedView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            currentFeedFilter = FeedFilter.Timeline,
            paddingValues = PaddingValues(),
            loadingState = inProgressFeedUpdateStatus,
            lazyListState = rememberLazyListState(),
            feedFontSizes = FeedFontSizes(),
            swipeActions = SwipeActions(
                leftSwipeAction = SwipeActionType.NONE,
                rightSwipeAction = SwipeActionType.NONE
            ),
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
            onDeleteFeedSourceClick = {},
            onPinFeedClick = {},
            markAllAsRead = {},
            onEditCategoryClick = { _, _ -> },
            onDeleteCategoryClick = { },
        )
    }
}
