package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.NoFeedSourcesStatus
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.desktop.ui.components.DesktopArticleList
import com.prof18.feedflow.desktop.ui.components.DesktopHomeToolbar
import com.prof18.feedflow.desktop.ui.components.FeedFlowVerticalScrollbar
import com.prof18.feedflow.desktop.ui.components.SnackbarHost
import com.prof18.feedflow.desktop.ui.components.articleCardContainerColor
import com.prof18.feedflow.desktop.ui.components.floatingSurface
import com.prof18.feedflow.desktop.ui.scrollToItemConditionally
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.coroutines.launch
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CircularProgressIndicator
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text

@Composable
@Suppress("UnusedParameter")
internal fun DesktopHomeScreenContent(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    snackbarHostState: DesktopFeedbackState,
    onSearchClick: () -> Unit,
    pendingNewArticlesCount: Int,
    onShowNewArticlesClicked: () -> Unit,
    onImportExportClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState(),
    showDrawerMenu: Boolean = true,
    isGridLayoutAllowed: Boolean = true,
    isDrawerOpen: Boolean = false,
    onDrawerMenuClick: () -> Unit = {},
    onEmptyStateClick: (() -> Unit)? = null,
    selectedArticleId: String? = null,
) {
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    var isGrid by remember { mutableStateOf(false) }
    val bodyFocusRequester = remember { FocusRequester() }
    val toolbarFocusRequester = remember { FocusRequester() }
    var newArticlesFocused by remember { mutableStateOf(false) }
    var scrollActionFocused by remember { mutableStateOf(false) }
    var keepScrollActionVisible by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    val showScrollToTop by remember {
        derivedStateOf {
            if (isGrid) {
                gridState.firstVisibleItemIndex > 1
            } else {
                listState.firstVisibleItemIndex > 1
            }
        }
    }
    fun scrollToTop(restoreFocus: Boolean = false) {
        if (restoreFocus) keepScrollActionVisible = true
        scope.launch {
            try {
                if (isGrid) {
                    gridState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotion)
                } else {
                    listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotion)
                }
                if (restoreFocus && scrollActionFocused) bodyFocusRequester.requestFocus()
            } finally {
                if (restoreFocus) keepScrollActionVisible = false
            }
        }
    }
    Column(modifier = modifier.background(JewelTheme.globalColors.panelBackground)) {
        DesktopHomeToolbar(
            currentFeedFilter = displayState.currentFeedFilter,
            unReadCount = displayState.unReadCount,
            isUnreadCountHidden = displayState.isUnreadCountHidden,
            showDrawerMenu = showDrawerMenu,
            isDrawerOpen = isDrawerOpen,
            onDrawerMenuClick = onDrawerMenuClick,
            onClick = { scrollToTop() },
            onDoubleClick = {
                scrollToTop()
                feedListActions.refreshData()
            },
            onSearchClick = onSearchClick,
            onRefreshClick = feedListActions.refreshData,
            isRefreshing = displayState.feedUpdateStatus.isLoading(),
            refreshEnabled = feedListActions.refreshEnabled,
            modifier = Modifier.focusRequester(toolbarFocusRequester),
        )
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(JewelTheme.globalColors.borders.normal.copy(alpha = 0.35f)),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.fillMaxSize().focusRequester(bodyFocusRequester)) {
                HomeFeedBody(
                    displayState = displayState,
                    feedListActions = feedListActions,
                    feedManagementActions = feedManagementActions,
                    shareBehavior = shareBehavior,
                    listState = listState,
                    gridState = gridState,
                    isGridLayoutAllowed = isGridLayoutAllowed,
                    onImportExportClick = onImportExportClick,
                    showDrawerMenu = showDrawerMenu,
                    onDrawerMenuClick = onDrawerMenuClick,
                    onGridArrangementChanged = { isGrid = it },
                    isGrid = isGrid,
                    selectedArticleId = selectedArticleId,
                )
            }
            if (pendingNewArticlesCount > 0) {
                DefaultButton(
                    onClick = {
                        if (newArticlesFocused) toolbarFocusRequester.requestFocus()
                        onShowNewArticlesClicked()
                    },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp).floatingSurface()
                        .heightIn(min = actionHeight)
                        .onFocusChanged { newArticlesFocused = it.isFocused },
                ) {
                    val count = if (pendingNewArticlesCount >= MaxNewArticlesCount) {
                        "$MaxNewArticlesCount+"
                    } else {
                        pendingNewArticlesCount.toString()
                    }
                    Text(strings.newArticlesButton(count))
                }
            }
            if (showScrollToTop || keepScrollActionVisible) {
                OutlinedButton(
                    onClick = {
                        if (!keepScrollActionVisible) scrollToTop(restoreFocus = scrollActionFocused)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).floatingSurface()
                        .heightIn(min = actionHeight)
                        .onFocusChanged { scrollActionFocused = it.isFocused },
                ) {
                    Text(strings.scrollToTopButtonContentDescription)
                }
            }
            SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun HomeFeedBody(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    isGridLayoutAllowed: Boolean,
    onImportExportClick: () -> Unit,
    showDrawerMenu: Boolean,
    onDrawerMenuClick: () -> Unit,
    onGridArrangementChanged: (Boolean) -> Unit,
    isGrid: Boolean,
    selectedArticleId: String?,
) {
    val strings = LocalFeedFlowStrings.current
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    when {
        displayState.feedUpdateStatus is NoFeedSourcesStatus -> Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = strings.noFeedsFoundMessage,
                    modifier = Modifier.semantics { heading() },
                )
                DefaultButton(
                    onClick = feedManagementActions.onAddFeedClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                ) {
                    Text(strings.addFeed)
                }
                OutlinedButton(
                    onClick = onImportExportClick,
                    modifier = Modifier.fillMaxWidth().heightIn(min = actionHeight),
                ) {
                    Text(strings.importExportLabel)
                }
            }
        }
        !displayState.feedUpdateStatus.isLoading() && displayState.feedItems.isEmpty() -> EmptyTimeline(
            currentFilter = displayState.currentFeedFilter,
            nextFeedState = displayState.nextFeedDisplayState,
            onRefresh = feedListActions.refreshData,
            onTimeline = feedListActions.onBackToTimelineClick,
            onOpenDrawer = onDrawerMenuClick.takeIf { showDrawerMenu },
            onNext = feedListActions.onNavigateNext,
        )
        else -> HomeArticleList(
            displayState = displayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            shareBehavior = shareBehavior,
            listState = listState,
            gridState = gridState,
            isGridLayoutAllowed = isGridLayoutAllowed,
            onGridArrangementChanged = onGridArrangementChanged,
            isGrid = isGrid,
            selectedArticleId = selectedArticleId,
        )
    }
}

@Composable
private fun HomeArticleList(
    displayState: HomeDisplayState,
    feedListActions: FeedListActions,
    feedManagementActions: FeedManagementActions,
    shareBehavior: ShareBehavior,
    listState: LazyListState,
    gridState: LazyStaggeredGridState,
    isGridLayoutAllowed: Boolean,
    onGridArrangementChanged: (Boolean) -> Unit,
    isGrid: Boolean,
    selectedArticleId: String?,
) {
    val cardLayout = displayState.feedLayout != FeedLayout.LIST
    Box(
        modifier = Modifier.fillMaxSize().then(
            if (cardLayout) Modifier.background(articleCardContainerColor()) else Modifier,
        ),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isGridArrangement = displayState.isGridLayoutEnabled && isGridLayoutAllowed && cardLayout
        val listModifier = if (isGridArrangement) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = listPaneMaxContentWidth).fillMaxWidth()
        }
        Column(modifier = listModifier.fillMaxHeight()) {
            if (displayState.feedUpdateStatus.isLoading()) {
                FeedLoadingProgress(displayState)
            }
            DesktopArticleList(
                selectedArticleId = selectedArticleId,
                feedItems = displayState.feedItems,
                nextFeedState = displayState.nextFeedDisplayState,
                feedFontSize = displayState.feedFontSizes,
                feedLayout = displayState.feedLayout,
                currentFeedFilter = displayState.currentFeedFilter,
                shareMenuLabel = shareBehavior.shareLinkTitle,
                shareCommentsMenuLabel = shareBehavior.shareCommentsTitle,
                swipeActions = displayState.swipeActions,
                onVisibleFeedItemsChanged = feedListActions.onVisibleFeedItemsChanged,
                requestMoreItems = feedListActions.requestNewData,
                onFeedItemClick = feedListActions::openAndMarkRead,
                onOpenInBrowser = feedListActions.openInBrowser,
                onBookmarkClick = feedListActions.updateBookmarkStatus,
                onReadStatusClick = feedListActions.updateReadStatus,
                onCommentClick = feedListActions::openAndMarkRead,
                markAllAsRead = feedListActions.markAllRead,
                onShareClick = shareBehavior.onShareClick,
                onOpenFeedSettings = feedManagementActions.onEditFeedClick,
                onOpenFeedWebsite = feedManagementActions.onOpenWebsite,
                onNavigateNext = feedListActions.onNavigateNext,
                onGridArrangementChanged = onGridArrangementChanged,
                listState = listState,
                gridState = gridState,
                contentPadding = PaddingValues(top = listPaneTopContentPadding),
                showNextFeedButton = true,
                isGridLayoutEnabled = displayState.isGridLayoutEnabled,
                isGridLayoutAllowed = isGridLayoutAllowed,
                onMarkAllAboveAsRead = feedListActions.markAllAboveAsRead,
                onMarkAllBelowAsRead = feedListActions.markAllBelowAsRead,
                markAllAsReadEnabled = feedListActions.bulkActionsEnabled,
                feedItemDisplaySettings = displayState.feedItemDisplaySettings,
                modifier = Modifier.weight(1f),
            )
        }
        if (!isGrid) {
            FeedFlowVerticalScrollbar(listState)
        }
    }
}

@Composable
private fun FeedLoadingProgress(displayState: HomeDisplayState) {
    val status = displayState.feedUpdateStatus
    val counter = if (status.refreshedFeedCount > 0 && status.totalFeedCount > 0) {
        "${status.refreshedFeedCount}/${status.totalFeedCount}"
    } else {
        "..."
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate },
        )
        Text(LocalFeedFlowStrings.current.loadingFeedMessage(counter))
    }
}

private fun FeedListActions.openAndMarkRead(urlInfo: FeedItemUrlInfo) {
    openUrl(urlInfo)
    markAsRead(FeedItemId(urlInfo.id))
}

@Composable
private fun EmptyTimeline(
    currentFilter: FeedFilter,
    nextFeedState: NextFeedDisplayState,
    onRefresh: () -> Unit,
    onTimeline: () -> Unit,
    onOpenDrawer: (() -> Unit)?,
    onNext: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    val isSpecialFilter = currentFilter is FeedFilter.Read || currentFilter is FeedFilter.Bookmarks
    val actionHeight = with(LocalDensity.current) { JewelTheme.defaultTextStyle.lineHeight.toDp() } + 16.dp
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            when (currentFilter) {
                FeedFilter.Read -> strings.readArticlesEmptyScreenMessage
                FeedFilter.Bookmarks -> strings.bookmarkedArticlesEmptyScreenMessage
                else -> strings.emptyFeedMessage
            },
        )
        DefaultButton(
            onClick = if (isSpecialFilter) onTimeline else onRefresh,
            modifier = Modifier.heightIn(min = actionHeight),
        ) {
            Text(if (isSpecialFilter) strings.emptyScreenBackToTimeline else strings.refreshFeeds)
        }
        if (onOpenDrawer != null) {
            OutlinedButton(onClick = onOpenDrawer, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(strings.openAnotherFeed)
            }
        }
        if (nextFeedState is NextFeedDisplayState.NextFeedDisplayEnabledState) {
            OutlinedButton(onClick = onNext, modifier = Modifier.heightIn(min = actionHeight)) {
                Text(nextFeedState.title)
            }
        }
    }
}

private const val MaxNewArticlesCount = 40
