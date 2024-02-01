package com.prof18.feedflow.desktop.home.bywindowsize

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
internal fun MediumView(
    navDrawerState: NavDrawerState,
    currentFeedFilter: FeedFilter,
    paddingValues: PaddingValues,
    loadingState: FeedUpdateStatus,
    feedItems: List<FeedItem>,
    lazyListState: LazyListState,
    unReadCount: Long,
    onAddFeedClick: () -> Unit,
    onFeedFilterSelected: (FeedFilter) -> Unit,
    refreshData: () -> Unit,
    requestNewData: () -> Unit,
    markAsReadOnScroll: (Int) -> Unit,
    markAsRead: (FeedItemId) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    onReadStatusClick: (FeedItemId, Boolean) -> Unit,
) {
    var isDrawerMenuFullVisible by remember {
        mutableStateOf(true)
    }
    val scope = rememberCoroutineScope()

    Row {
        AnimatedVisibility(
            modifier = Modifier
                .weight(1f),
            visible = isDrawerMenuFullVisible && (feedItems.isNotEmpty() || navDrawerState.isNotEmpty()),
        ) {
            Scaffold { paddingValues ->
                Drawer(
                    modifier = Modifier
                        .padding(paddingValues),
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
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                )
            }
        }

        HomeScreenContent(
            paddingValues = paddingValues,
            loadingState = loadingState,
            feedState = feedItems,
            listState = lazyListState,
            unReadCount = unReadCount,
            showDrawerMenu = true,
            isDrawerMenuOpen = isDrawerMenuFullVisible,
            currentFeedFilter = currentFeedFilter,
            modifier = Modifier
                .weight(2f),
            onDrawerMenuClick = {
                isDrawerMenuFullVisible = !isDrawerMenuFullVisible
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
        )
    }
}

@Preview
@Composable
private fun MediumViewPreview() {
    FeedFlowTheme {
        MediumView(
            feedItems = feedItemsForPreview,
            navDrawerState = navDrawerState,
            unReadCount = 42,
            currentFeedFilter = FeedFilter.Timeline,
            paddingValues = PaddingValues(),
            loadingState = inProgressFeedUpdateStatus,
            lazyListState = rememberLazyListState(),
            onAddFeedClick = {},
            onFeedFilterSelected = {},
            refreshData = {},
            requestNewData = {},
            markAsReadOnScroll = {},
            markAsRead = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
        )
    }
}
