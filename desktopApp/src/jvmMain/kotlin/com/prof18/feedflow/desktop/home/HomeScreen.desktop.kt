package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveArticleOpenMode
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.main.LocalDesktopMenuCommands
import com.prof18.feedflow.desktop.search.SearchScreen
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.FeedListActions
import com.prof18.feedflow.desktop.ui.FeedManagementActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.LocalReduceMotion
import com.prof18.feedflow.desktop.ui.ShareBehavior
import com.prof18.feedflow.desktop.ui.components.DesktopLoadingOperationDialog
import com.prof18.feedflow.desktop.ui.desktopExternalLinkAction
import com.prof18.feedflow.desktop.ui.scrollToItemConditionally
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.model.HomeDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewDisabledState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewEnabledState
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    listStateStore: DesktopHomeListStateStore,
    snackbarHostState: DesktopFeedbackState,
    onImportExportClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    onAddFeedClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onFeedSuggestionsClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val changeFeedCategoryViewModel = koinViewModel<ChangeFeedCategoryViewModel>()
    val homeSettingsRepository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }
    val isMultiPaneLayoutEnabled by homeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsState()
    val listState = listStateStore.getListState(isMultiPaneLayoutEnabled)
    val gridState = rememberLazyStaggeredGridState()

    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()
    val navDrawerState by homeViewModel.navDrawerState.collectAsState()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsState(initial = 0)
    val isUnreadCountHidden by homeViewModel.isUnreadCountHidden.collectAsState()
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsState()
    val swipeActions by homeViewModel.swipeActions.collectAsState()
    val feedOperation by homeViewModel.feedOperationState.collectAsState()
    val feedLayout by homeViewModel.feedLayout.collectAsState()
    val isGridLayoutEnabled by homeViewModel.isGridLayoutEnabled.collectAsState()
    val feedItemDisplaySettings by homeViewModel.feedItemDisplaySettings.collectAsState()
    val nextFeedPreviewState by homeViewModel.nextFeedPreviewState.collectAsState()
    val refreshTrigger by homeViewModel.refreshTriggerState.collectAsState()
    val pendingNewArticlesCount by homeViewModel.pendingNewArticlesState.collectAsState()
    val commands = desktopWorkspaceCommandAvailability(loadingState, feedOperation)

    val readerModeViewModel = koinViewModel<ReaderModeViewModel>()
    val currentReaderArticle by readerModeViewModel.currentArticleState.collectAsState()

    var showNoFeedsBottomSheet by remember { mutableStateOf(false) }

    val browserManager = DI.koin.get<BrowserManager>()
    val strings = LocalFeedFlowStrings.current
    val currentErrorStrings by rememberUpdatedState(strings)
    val currentOnAccountsClick by rememberUpdatedState(onAccountsClick)
    val uriHandler = LocalUriHandler.current
    fun showBrowserLaunchError() {
        scope.launch {
            snackbarHostState.showFeedback(
                message = strings.browserLaunchError,
                duration = DesktopFeedbackDuration.Short,
            )
        }
    }

    if (feedOperation != FeedOperation.None) {
        DesktopLoadingOperationDialog(feedOperation)
    }

    LaunchedEffect(homeViewModel, snackbarHostState) {
        homeViewModel.errorState.collect { errorState ->
            when (errorState) {
                is UIErrorState.DatabaseError -> {
                    snackbarHostState.showFeedback(
                        message = strings.databaseError(errorState.errorCode.code),
                        duration = DesktopFeedbackDuration.Short,
                    )
                }

                is UIErrorState.SyncError -> {
                    showDesktopSyncFailureFeedback(
                        state = snackbarHostState,
                        errorCode = errorState.errorCode,
                        strings = currentErrorStrings,
                        onRetryRefresh = desktopWorkspaceAction(
                            DesktopWorkspaceCommand.Refresh,
                            homeViewModel::desktopWorkspaceCommandAvailability,
                            homeViewModel::refreshFeeds,
                        ),
                        onOpenAccounts = { currentOnAccountsClick() },
                    )
                }

                is UIErrorState.DeleteFeedSourceError -> {
                    snackbarHostState.showFeedback(
                        message = strings.deleteFeedSourceError,
                        duration = DesktopFeedbackDuration.Short,
                    )
                }
            }
        }
    }

    val homeDisplayState = remember(
        feedState,
        navDrawerState,
        unReadCount,
        isUnreadCountHidden,
        loadingState,
        feedFontSizes,
        currentFeedFilter,
        swipeActions,
        feedLayout,
        isGridLayoutEnabled,
        nextFeedPreviewState,
        feedItemDisplaySettings,
    ) {
        HomeDisplayState(
            feedItems = feedState,
            navDrawerState = navDrawerState,
            unReadCount = unReadCount,
            isUnreadCountHidden = isUnreadCountHidden,
            feedUpdateStatus = loadingState,
            feedFontSizes = feedFontSizes,
            currentFeedFilter = currentFeedFilter,
            swipeActions = swipeActions,
            feedLayout = feedLayout,
            isGridLayoutEnabled = isGridLayoutEnabled,
            nextFeedDisplayState = nextFeedPreviewState.asDisplayState(),
            feedItemDisplaySettings = feedItemDisplaySettings,
        )
    }

    val openReaderArticle: (FeedItemUrlInfo) -> Unit = remember(readerModeViewModel) {
        readerModeViewModel::loadReaderContent
    }
    val resetReaderArticle: () -> Unit = remember(readerModeViewModel) {
        readerModeViewModel::resetState
    }

    val reduceMotionEnabled = LocalReduceMotion.current
    var inlineSearch by rememberSaveable { mutableStateOf(false) }
    val startSearch: () -> Unit = {
        if (isMultiPaneLayoutEnabled) inlineSearch = true else onSearchClick()
    }
    val menuCommands = LocalDesktopMenuCommands.current
    val latestSearch by rememberUpdatedState(startSearch)
    DisposableEffect(menuCommands) {
        menuCommands.search = { latestSearch() }
        onDispose { menuCommands.search = null }
    }

    LaunchedEffect(refreshTrigger, isMultiPaneLayoutEnabled) {
        if (refreshTrigger > 0) {
            if (isGridLayoutEnabled && !isMultiPaneLayoutEnabled) {
                gridState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
            } else {
                listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
            }
            if (isMultiPaneLayoutEnabled) {
                resetReaderArticle()
            }
        }
    }

    val feedListActions = remember(
        homeViewModel,
        resetReaderArticle,
        browserManager,
        uriHandler,
        isMultiPaneLayoutEnabled,
        openReaderArticle,
        navigateToReaderMode,
        scope,
        snackbarHostState,
        strings,
        listState,
        commands,
    ) {
        FeedListActions(
            onClearOldArticlesClicked = desktopWorkspaceAction(
                DesktopWorkspaceCommand.BulkAction,
                homeViewModel::desktopWorkspaceCommandAvailability,
                homeViewModel::deleteOldFeedItems,
            ),
            onDeleteDatabaseClick = desktopWorkspaceAction(
                DesktopWorkspaceCommand.BulkAction,
                homeViewModel::desktopWorkspaceCommandAvailability,
                homeViewModel::deleteAllFeeds,
            ),
            refreshData = desktopWorkspaceAction(
                DesktopWorkspaceCommand.Refresh,
                homeViewModel::desktopWorkspaceCommandAvailability,
                homeViewModel::refreshFeeds,
            ),
            requestNewData = { homeViewModel.requestNewFeedsPage() },
            markAllRead = desktopWorkspaceAction(
                DesktopWorkspaceCommand.BulkAction,
                homeViewModel::desktopWorkspaceCommandAvailability,
                homeViewModel::markAllRead,
            ),
            onBackToTimelineClick = {
                resetReaderArticle()
                homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
            },
            onVisibleFeedItemsChanged = homeViewModel::onVisibleFeedItemsChanged,
            markAsRead = { feedItemId -> homeViewModel.markAsRead(feedItemId.id) },
            openUrl = { feedItemUrlInfo ->
                handleOpenUrlForDesktop(
                    feedItemUrlInfo = feedItemUrlInfo,
                    browserManager = browserManager,
                    uriHandler = uriHandler,
                    onOpenReaderArticle = if (isMultiPaneLayoutEnabled) {
                        openReaderArticle
                    } else {
                        navigateToReaderMode
                    },
                    onBrowserError = ::showBrowserLaunchError,
                )
            },
            openInBrowser = { feedItemUrlInfo ->
                if (!uriHandler.openUriSafely(feedItemUrlInfo.url)) {
                    showBrowserLaunchError()
                }
            },
            updateBookmarkStatus = { feedItemId, isBookmarked ->
                homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
            },
            updateReadStatus = { feedItemId, isRead -> homeViewModel.updateReadStatus(feedItemId, isRead) },
            markAllAboveAsRead = { feedItemId -> homeViewModel.markAllAboveAsRead(feedItemId) },
            markAllBelowAsRead = { feedItemId -> homeViewModel.markAllBelowAsRead(feedItemId) },
            onNavigateNext = {
                scope.launch { listState.scrollToItem(0) }
                homeViewModel.onNavigateToNextFeed()
            },
            refreshEnabled = commands.refreshEnabled,
            bulkActionsEnabled = commands.bulkActionsEnabled,
        )
    }

    val feedManagementActions = remember(
        homeViewModel,
        resetReaderArticle,
        onAddFeedClick,
        onFeedSuggestionsClick,
        onImportExportClick,
        onEditFeedClick,
        changeFeedCategoryViewModel,
        uriHandler,
        scope,
        snackbarHostState,
        strings,
    ) {
        FeedManagementActions(
            onAddFeedClick = onAddFeedClick,
            onFeedFilterSelected = { feedFilter ->
                resetReaderArticle()
                homeViewModel.onFeedFilterSelected(feedFilter)
            },
            onEditFeedClick = onEditFeedClick,
            onDeleteFeedSourceClick = { feedSource -> homeViewModel.deleteFeedSource(feedSource) },
            onPinFeedClick = { feedSource -> homeViewModel.toggleFeedPin(feedSource) },
            onEditCategoryClick = { categoryId, newName -> homeViewModel.updateCategoryName(categoryId, newName) },
            validateCategoryName = homeViewModel::validateCategoryName,
            onDeleteCategoryClick = { categoryId -> homeViewModel.deleteCategory(categoryId) },
            onOpenWebsite = desktopExternalLinkAction(uriHandler, ::showBrowserLaunchError),
            onMoveFeedSourcesToCategory = { feedSources, category ->
                changeFeedCategoryViewModel.moveFeedSourcesToCategory(feedSources, category)
            },
            onDeleteAllFeedsInCategoryClick = { feedSources ->
                homeViewModel.deleteAllFeedsInCategory(feedSources)
            },
            onDeleteAllFeedsInCategoryByIdClick = { categoryId ->
                homeViewModel.deleteAllFeedsInCategory(categoryId)
            },
            onReorderPinnedFeedSources = { feedSources ->
                homeViewModel.reorderPinnedFeedSources(feedSources)
            },
            onReorderCategories = { categoryWrappers ->
                homeViewModel.reorderCategories(categoryWrappers)
            },
            onReorderFeedSources = { feedSources ->
                homeViewModel.reorderFeedSources(feedSources)
            },
            onMarkAllReadForFeedSourceClick = { feedSource ->
                homeViewModel.markAllReadForFeedSource(feedSource)
            },
            onMarkAllReadForCategoryClick = { category ->
                homeViewModel.markAllReadForCategory(category)
            },
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            onImportFeedsClick = onImportExportClick,
        )
    }

    val shareBehavior = remember(scope, snackbarHostState, strings) {
        ShareBehavior(
            onShareClick = { urlTitle ->
                copyToClipboard(urlTitle.url)
                scope.launch {
                    snackbarHostState.showFeedback(message = strings.linkCopiedSuccess)
                }
            },
            shareLinkTitle = strings.menuCopyLink,
            shareCommentsTitle = strings.menuCopyLinkComments,
        )
    }

    if (isMultiPaneLayoutEnabled) {
        DesktopHomeScaffold(
            listState = listState,
            gridState = gridState,
            onSearchClick = startSearch,
            searchContent = if (inlineSearch) {
                {
                    SearchScreen(
                        navigateBack = { inlineSearch = false },
                        navigateToReaderMode = openReaderArticle,
                        navigateToEditFeed = onEditFeedClick,
                    )
                }
            } else {
                null
            },
            pendingNewArticlesCount = pendingNewArticlesCount,
            onShowNewArticlesClicked = homeViewModel::onShowNewArticlesClicked,
            displayState = homeDisplayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            snackbarHostState = snackbarHostState,
            shareBehavior = shareBehavior,
            onImportExportClick = onImportExportClick,
            currentReaderArticle = currentReaderArticle,
            onReaderClosed = { readerModeViewModel.resetState() },
            onEmptyStateClick = {
                showNoFeedsBottomSheet = true
            },
        )
    } else {
        DesktopSinglePaneHomeScaffold(
            listState = listState,
            gridState = gridState,
            onSearchClick = onSearchClick,
            pendingNewArticlesCount = pendingNewArticlesCount,
            onShowNewArticlesClicked = homeViewModel::onShowNewArticlesClicked,
            displayState = homeDisplayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            snackbarHostState = snackbarHostState,
            shareBehavior = shareBehavior,
            onImportExportClick = onImportExportClick,
            onEmptyStateClick = {
                showNoFeedsBottomSheet = true
            },
        )
    }

    NoFeedsDialog(
        showDialog = showNoFeedsBottomSheet,
        onDismissRequest = {
            showNoFeedsBottomSheet = false
        },
        onImportExportClick = {
            showNoFeedsBottomSheet = false
            onImportExportClick()
        },
        onAccountsClick = {
            showNoFeedsBottomSheet = false
            onAccountsClick()
        },
        onAddFeedClick = {
            showNoFeedsBottomSheet = false
            onAddFeedClick()
        },
        onFeedSuggestionsClick = {
            showNoFeedsBottomSheet = false
            onFeedSuggestionsClick()
        },
    )
}

private fun handleOpenUrlForDesktop(
    feedItemUrlInfo: FeedItemUrlInfo,
    browserManager: BrowserManager,
    uriHandler: UriHandler,
    onOpenReaderArticle: (FeedItemUrlInfo) -> Unit,
    onBrowserError: () -> Unit,
) {
    fun openUri(url: String) {
        if (!uriHandler.openUriSafely(url)) {
            onBrowserError()
        }
    }

    val openMode = feedItemUrlInfo.resolveArticleOpenMode(browserManager.getArticleOpenMode())
    when {
        openMode.isReaderMode() -> {
            onOpenReaderArticle(feedItemUrlInfo)
        }

        else -> {
            openUri(feedItemUrlInfo.url)
        }
    }
}

private fun NextFeedPreviewState.asDisplayState(): NextFeedDisplayState = when (this) {
    is NextFeedPreviewDisabledState -> NextFeedDisplayState.NextFeedDisplayDisabledState
    is NextFeedPreviewEnabledState -> NextFeedDisplayState.NextFeedDisplayEnabledState(this.title)
}
