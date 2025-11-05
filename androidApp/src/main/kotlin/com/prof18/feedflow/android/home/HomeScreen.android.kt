package com.prof18.feedflow.android.home

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.home.AdaptiveHomeView
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.WindowSizeClass
import com.prof18.feedflow.shared.ui.home.components.LoadingOperationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.koinInject

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onImportExportClick: () -> Unit = {},
    onEditFeedClick: (FeedSource) -> Unit,
) {
    val browserManager = koinInject<BrowserManager>()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val navDrawerState by homeViewModel.navDrawerState.collectAsStateWithLifecycle()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsStateWithLifecycle()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsStateWithLifecycle()
    val swipeActions by homeViewModel.swipeActions.collectAsStateWithLifecycle()
    val feedOperation by homeViewModel.feedOperationState.collectAsStateWithLifecycle()
    val feedLayout by homeViewModel.feedLayout.collectAsStateWithLifecycle()
    val isSyncUploadRequired by homeViewModel.isSyncUploadRequired.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current

    if (feedOperation != FeedOperation.None) {
        LoadingOperationDialog(feedOperation)
    }

    LaunchedEffect(Unit) {
        homeViewModel.errorState.collect { errorState ->
            when (errorState) {
                is UIErrorState.DatabaseError -> {
                    snackbarHostState.showSnackbar(
                        strings.databaseError(errorState.errorCode.code),
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.FeedErrorState -> {
                    snackbarHostState.showSnackbar(
                        strings.feedErrorMessageImproved(errorState.feedName),
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.SyncError -> {
                    snackbarHostState.showSnackbar(
                        strings.syncErrorMessage(errorState.errorCode.code),
                        duration = SnackbarDuration.Short,
                    )
                }

                is UIErrorState.DeleteFeedSourceError -> {
                    snackbarHostState.showSnackbar(
                        strings.deleteFeedSourceError,
                        duration = SnackbarDuration.Short,
                    )
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
            onAccountsClick = onAccountsClick,
        )
    }

    val homeDisplayState = HomeDisplayState(
        feedItems = feedState,
        navDrawerState = navDrawerState,
        unReadCount = unReadCount,
        feedUpdateStatus = loadingState,
        feedFontSizes = feedFontSizes,
        currentFeedFilter = currentFeedFilter,
        swipeActions = swipeActions,
        feedLayout = feedLayout,
        isSyncUploadRequired = isSyncUploadRequired,
    )

    val feedListActions = FeedListActions(
        onClearOldArticlesClicked = { homeViewModel.deleteOldFeedItems() },
        onDeleteDatabaseClick = { homeViewModel.deleteAllFeeds() },
        refreshData = { homeViewModel.getNewFeeds() },
        requestNewData = { homeViewModel.requestNewFeedsPage() },
        forceRefreshData = { homeViewModel.forceFeedRefresh() },
        markAllRead = { homeViewModel.markAllRead() },
        onBackToTimelineClick = { homeViewModel.onFeedFilterSelected(FeedFilter.Timeline) },
        markAsReadOnScroll = { lastVisibleIndex -> homeViewModel.markAsReadOnScroll(lastVisibleIndex) },
        markAsRead = { feedItemId -> homeViewModel.markAsRead(feedItemId.id) },
        openUrl = { urlInfo -> openUrl(urlInfo, navigateToReaderMode, browserManager, context) },
        updateBookmarkStatus = { feedItemId, isBookmarked ->
            homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
        },
        updateReadStatus = { feedItemId, isRead -> homeViewModel.updateReadStatus(feedItemId, isRead) },
        markAllAboveAsRead = { feedItemId -> homeViewModel.markAllAboveAsRead(feedItemId) },
        markAllBelowAsRead = { feedItemId -> homeViewModel.markAllBelowAsRead(feedItemId) },
    )

    val feedManagementActions = FeedManagementActions(
        onAddFeedClick = { showBottomSheet = true },
        onFeedFilterSelected = { feedFilter -> homeViewModel.onFeedFilterSelected(feedFilter) },
        onEditFeedClick = onEditFeedClick,
        onDeleteFeedSourceClick = { feedSource -> homeViewModel.deleteFeedSource(feedSource) },
        onPinFeedClick = { feedSource -> homeViewModel.toggleFeedPin(feedSource) },
        onEditCategoryClick = { categoryId, newName -> homeViewModel.updateCategoryName(categoryId, newName) },
        onDeleteCategoryClick = { categoryId -> homeViewModel.deleteCategory(categoryId) },
        onOpenWebsite = { url -> browserManager.openUrlWithFavoriteBrowser(url, context) },
    )

    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val adaptiveWindowSizeClass = when (windowSizeClass.windowWidthSizeClass) {
        androidx.window.core.layout.WindowWidthSizeClass.COMPACT -> WindowSizeClass.Compact
        androidx.window.core.layout.WindowWidthSizeClass.MEDIUM -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }

    AdaptiveHomeView(
        snackbarHostState = snackbarHostState,
        onSettingsButtonClicked = onSettingsButtonClicked,
        onSearchClick = onSearchClick,
        displayState = homeDisplayState,
        feedListActions = feedListActions,
        feedManagementActions = feedManagementActions,
        windowSizeClass = adaptiveWindowSizeClass,
        showDropdownMenu = true,
        feedContentWrapper = { content ->
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = homeDisplayState.feedUpdateStatus.isLoading(),
                onRefresh = feedListActions.refreshData,
            ) {
                content()
            }
        },
        shareBehavior = ShareBehavior(
            onShareClick = { titleAndUrl ->
                context.openShareSheet(
                    title = titleAndUrl.title,
                    url = titleAndUrl.url,
                )
            },
            shareLinkTitle = strings.menuShare,
            shareCommentsTitle = strings.menuShareComments,
        ),
        onBackupClick = homeViewModel::enqueueBackup,
    )
}

private fun openUrl(
    urlInfo: FeedItemUrlInfo,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    browserManager: BrowserManager,
    context: Context,
) {
    when (urlInfo.linkOpeningPreference) {
        LinkOpeningPreference.READER_MODE -> navigateToReaderMode(urlInfo)
        LinkOpeningPreference.INTERNAL_BROWSER -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
        LinkOpeningPreference.PREFERRED_BROWSER -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
        LinkOpeningPreference.DEFAULT -> {
            if (browserManager.openReaderMode() && !urlInfo.shouldOpenInBrowser()) {
                navigateToReaderMode(urlInfo)
            } else {
                browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
            }
        }
    }
}
