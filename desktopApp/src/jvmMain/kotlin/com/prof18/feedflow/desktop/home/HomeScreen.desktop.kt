package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.home.bywindowsize.CompactView
import com.prof18.feedflow.desktop.home.bywindowsize.ExpandedView
import com.prof18.feedflow.desktop.home.bywindowsize.MediumView
import com.prof18.feedflow.desktop.home.components.NoFeedsDialog
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.desktop.utils.WindowSizeClass
import com.prof18.feedflow.desktop.utils.WindowWidthSizeClass
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.home.components.DeleteOldFeedDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    paddingValues: PaddingValues,
    homeViewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    onImportExportClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()
    val navDrawerState by homeViewModel.navDrawerState.collectAsState()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsState(initial = 0)
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsState()
    val isDeleting by homeViewModel.isDeletingState.collectAsState()

    val browserManager = DI.koin.get<BrowserManager>()
    val strings = LocalFeedFlowStrings.current

    if (isDeleting) {
        DeleteOldFeedDialog()
    }

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

                UIErrorState.SyncError -> {
                    snackbarHostState.showSnackbar(
                        strings.syncErrorMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    NoFeedsDialog(
        showDialog = showDialog,
        onDismissRequest = {
            showDialog = false
        },
        onImportExportClick = onImportExportClick,
        onAccountsClick = onAccountsClick,
    )

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> {
            CompactView(
                feedItems = feedState,
                navDrawerState = navDrawerState,
                unReadCount = unReadCount,
                currentFeedFilter = currentFeedFilter,
                paddingValues = paddingValues,
                loadingState = loadingState,
                lazyListState = listState,
                feedFontSizes = feedFontSizes,
                onAddFeedClick = {
                    showDialog = true
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                onBookmarkClick = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onReadStatusClick = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
                openUrl = { feedItemUrlInfo ->
                    when (feedItemUrlInfo.linkOpeningPreference) {
                        LinkOpeningPreference.READER_MODE -> navigateToReaderMode(feedItemUrlInfo)
                        LinkOpeningPreference.INTERNAL_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.PREFERRED_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.DEFAULT -> {
                            if (browserManager.openReaderMode()) {
                                navigateToReaderMode(feedItemUrlInfo)
                            } else {
                                openInBrowser(feedItemUrlInfo.url)
                            }
                        }
                    }
                },
                onDeleteFeedSourceClick = { feedSource ->
                    homeViewModel.deleteFeedSource(feedSource)
                },
                onPinFeedClick = { feedSource ->
                    homeViewModel.toggleFeedPin(feedSource)
                },
                markAllAsRead = {
                    homeViewModel.markAllRead()
                },
            )
        }

        WindowWidthSizeClass.Medium -> {
            MediumView(
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedItems = feedState,
                lazyListState = listState,
                unReadCount = unReadCount,
                feedFontSizes = feedFontSizes,
                onAddFeedClick = {
                    showDialog = true
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                onBookmarkClick = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onReadStatusClick = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
                openUrl = { feedItemUrlInfo ->
                    when (feedItemUrlInfo.linkOpeningPreference) {
                        LinkOpeningPreference.READER_MODE -> navigateToReaderMode(feedItemUrlInfo)
                        LinkOpeningPreference.INTERNAL_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.PREFERRED_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.DEFAULT -> {
                            if (browserManager.openReaderMode()) {
                                navigateToReaderMode(feedItemUrlInfo)
                            } else {
                                openInBrowser(feedItemUrlInfo.url)
                            }
                        }
                    }
                },
                onDeleteFeedSourceClick = { feedSource ->
                    homeViewModel.deleteFeedSource(feedSource)
                },
                onPinFeedClick = { feedSource ->
                    homeViewModel.toggleFeedPin(feedSource)
                },
                markAllAsRead = {
                    homeViewModel.markAllRead()
                },
            )
        }

        WindowWidthSizeClass.Expanded -> {
            ExpandedView(
                navDrawerState = navDrawerState,
                currentFeedFilter = currentFeedFilter,
                paddingValues = paddingValues,
                loadingState = loadingState,
                feedItems = feedState,
                lazyListState = listState,
                unReadCount = unReadCount,
                feedFontSizes = feedFontSizes,
                onAddFeedClick = {
                    showDialog = true
                },
                onFeedFilterSelected = { feedFilter ->
                    homeViewModel.onFeedFilterSelected(feedFilter)
                },
                refreshData = {
                    homeViewModel.getNewFeeds()
                },
                requestNewData = {
                    homeViewModel.requestNewFeedsPage()
                },
                markAsReadOnScroll = { lastVisibleIndex ->
                    homeViewModel.markAsReadOnScroll(lastVisibleIndex)
                },
                markAsRead = { feedItemId ->
                    homeViewModel.markAsRead(feedItemId.id)
                },
                onBookmarkClick = { feedItemId, isBookmarked ->
                    homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
                },
                onReadStatusClick = { feedItemId, isRead ->
                    homeViewModel.updateReadStatus(feedItemId, isRead)
                },
                onBackToTimelineClick = {
                    homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
                },
                onSearchClick = onSearchClick,
                openUrl = { feedItemUrlInfo ->
                    when (feedItemUrlInfo.linkOpeningPreference) {
                        LinkOpeningPreference.READER_MODE -> navigateToReaderMode(feedItemUrlInfo)
                        LinkOpeningPreference.INTERNAL_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.PREFERRED_BROWSER -> openInBrowser(feedItemUrlInfo.url)
                        LinkOpeningPreference.DEFAULT -> {
                            if (browserManager.openReaderMode()) {
                                navigateToReaderMode(feedItemUrlInfo)
                            } else {
                                openInBrowser(feedItemUrlInfo.url)
                            }
                        }
                    }
                },
                onDeleteFeedSourceClick = { feedSource ->
                    homeViewModel.deleteFeedSource(feedSource)
                },
                onPinFeedClick = { feedSource ->
                    homeViewModel.toggleFeedPin(feedSource)
                },
                markAllAsRead = {
                    homeViewModel.markAllRead()
                },
            )
        }
    }
}
