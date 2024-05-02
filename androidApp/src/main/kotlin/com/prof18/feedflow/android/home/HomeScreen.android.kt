package com.prof18.feedflow.android.home

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.home.bywindowsize.CompactHomeView
import com.prof18.feedflow.android.home.bywindowsize.ExpandedHomeView
import com.prof18.feedflow.android.home.bywindowsize.MediumHomeView
import com.prof18.feedflow.android.home.components.NoFeedsBottomSheet
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
internal fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit = {},
    onSearchClick: () -> Unit,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val browserManager = koinInject<BrowserManager>()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val navDrawerState by homeViewModel.navDrawerState.collectAsStateWithLifecycle()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsStateWithLifecycle()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsStateWithLifecycle(initialValue = 0)

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current
    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            when (errorState) {
                UIErrorState.DatabaseError -> {
                    snackbarHostState.showSnackbar(
                        strings.databaseError,
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.FeedErrorState -> {
                    snackbarHostState.showSnackbar(
                        strings.feedErrorMessage(errorState.feedName),
                        duration = SnackbarDuration.Short,
                    )
                }

                null -> {
                    // Do nothing
                }
            }
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    if (showBottomSheet) {
        NoFeedsBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            onAddFeedClick = onAddFeedClick,
            onImportExportClick = onImportExportClick,
        )
    }

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactHomeView(
                feedItems = feedState,
                navDrawerState = navDrawerState,
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                snackbarHostState = snackbarHostState,
                feedUpdateStatus = loadingState,
                currentFeedFilter = currentFeedFilter,
                onAddFeedClick = {
                    showBottomSheet = true
                },
                onClearOldArticlesClicked = {
                    homeViewModel.deleteOldFeedItems()
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                forceRefreshData = {
                    homeViewModel.forceFeedRefresh()
                },
                onDeleteDatabaseClick = {
                    homeViewModel.deleteAllFeeds()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                markAllRead = {
                    homeViewModel.markAllRead()
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                openUrl = { urlInfo ->
                    if (browserManager.openReaderMode() && !urlInfo.openOnlyOnBrowser) {
                        navigateToReaderMode(urlInfo)
                    } else {
                        browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
                    }
                },
                updateReadStatus = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                updateBookmarkStatus = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
            )
        }

        WindowWidthSizeClass.Medium -> {
            MediumHomeView(
                feedItems = feedState,
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                snackbarHostState = snackbarHostState,
                feedUpdateStatus = loadingState,
                onAddFeedClick = {
                    showBottomSheet = true
                },
                onClearOldArticlesClicked = {
                    homeViewModel.deleteOldFeedItems()
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                forceRefreshData = {
                    homeViewModel.forceFeedRefresh()
                },
                onDeleteDatabaseClick = {
                    homeViewModel.deleteAllFeeds()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                markAllRead = {
                    homeViewModel.markAllRead()
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                openUrl = { urlInfo ->
                    if (browserManager.openReaderMode() && !urlInfo.openOnlyOnBrowser) {
                        navigateToReaderMode(urlInfo)
                    } else {
                        browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
                    }
                },
                updateReadStatus = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                updateBookmarkStatus = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
            )
        }

        WindowWidthSizeClass.Expanded -> {
            ExpandedHomeView(
                feedItems = feedState,
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                unReadCount = unReadCount,
                onSettingsButtonClicked = onSettingsButtonClicked,
                snackbarHostState = snackbarHostState,
                feedUpdateStatus = loadingState,
                onAddFeedClick = {
                    showBottomSheet = true
                },
                onClearOldArticlesClicked = {
                    homeViewModel.deleteOldFeedItems()
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                forceRefreshData = {
                    homeViewModel.forceFeedRefresh()
                },
                onDeleteDatabaseClick = {
                    homeViewModel.deleteAllFeeds()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                markAllRead = {
                    homeViewModel.markAllRead()
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                openUrl = { urlInfo ->
                    if (browserManager.openReaderMode() && !urlInfo.openOnlyOnBrowser) {
                        navigateToReaderMode(urlInfo)
                    } else {
                        browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
                    }
                },
                updateReadStatus = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                updateBookmarkStatus = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
            )
        }
    }
}
