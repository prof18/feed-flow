package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.categoryselection.EditCategoryDialog
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.sanitizeUrl
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewDisabledState
import com.prof18.feedflow.shared.presentation.model.NextFeedPreviewState.NextFeedPreviewEnabledState
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.NextFeedDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.components.LoadingOperationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.LocalReduceMotion
import com.prof18.feedflow.shared.ui.utils.scrollToItemConditionally
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
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
    val listState = remember(isMultiPaneLayoutEnabled) { LazyListState() }

    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()
    val navDrawerState by homeViewModel.navDrawerState.collectAsState()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsState(initial = 0)
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsState()
    val swipeActions by homeViewModel.swipeActions.collectAsState()
    val feedOperation by homeViewModel.feedOperationState.collectAsState()
    val feedLayout by homeViewModel.feedLayout.collectAsState()
    val feedItemDisplaySettings by homeViewModel.feedItemDisplaySettings.collectAsState()
    val nextFeedPreviewState by homeViewModel.nextFeedPreviewState.collectAsState()
    val refreshTrigger by homeViewModel.refreshTriggerState.collectAsState()

    val categoriesState by changeFeedCategoryViewModel.categoriesState.collectAsState()
    val readerModeViewModel = koinViewModel<ReaderModeViewModel>()
    val currentReaderArticle by readerModeViewModel.currentArticleState.collectAsState()

    var showChangeCategorySheet by remember { mutableStateOf(false) }
    var showNoFeedsBottomSheet by remember { mutableStateOf(false) }

    val browserManager = DI.koin.get<BrowserManager>()
    val strings = LocalFeedFlowStrings.current
    val uriHandler = LocalUriHandler.current

    if (feedOperation != FeedOperation.None) {
        LoadingOperationDialog(feedOperation)
    }

    LaunchedEffect(Unit) {
        changeFeedCategoryViewModel.categoryChangedState.collect {
            showChangeCategorySheet = false
        }
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

    val homeDisplayState = HomeDisplayState(
        feedItems = feedState,
        navDrawerState = navDrawerState,
        unReadCount = unReadCount,
        feedUpdateStatus = loadingState,
        feedFontSizes = feedFontSizes,
        currentFeedFilter = currentFeedFilter,
        swipeActions = swipeActions,
        feedLayout = feedLayout,
        nextFeedDisplayState = nextFeedPreviewState.asDisplayState(),
        feedItemDisplaySettings = feedItemDisplaySettings,
    )

    val openReaderArticle: (FeedItemUrlInfo) -> Unit = { article ->
        readerModeViewModel.getReaderModeHtml(article)
    }
    val resetReaderArticle: () -> Unit = {
        readerModeViewModel.resetState()
    }

    val reduceMotionEnabled = LocalReduceMotion.current

    LaunchedEffect(refreshTrigger, isMultiPaneLayoutEnabled) {
        if (refreshTrigger > 0) {
            listState.scrollToItemConditionally(0, reduceMotionEnabled = reduceMotionEnabled)
            if (isMultiPaneLayoutEnabled) {
                resetReaderArticle()
            }
        }
    }

    val feedListActions = FeedListActions(
        onClearOldArticlesClicked = { homeViewModel.deleteOldFeedItems() },
        onDeleteDatabaseClick = { homeViewModel.deleteAllFeeds() },
        refreshData = { homeViewModel.refreshFeeds() },
        requestNewData = { homeViewModel.requestNewFeedsPage() },
        forceRefreshData = { homeViewModel.forceRefreshFeeds() },
        markAllRead = { homeViewModel.markAllRead() },
        onBackToTimelineClick = {
            resetReaderArticle()
            homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
        },
        markAsReadOnScroll = { lastVisibleIndex -> homeViewModel.markAsReadOnScroll(lastVisibleIndex) },
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
                onBrowserError = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            strings.browserLaunchError,
                            duration = SnackbarDuration.Short,
                        )
                    }
                },
            )
        },
        openInBrowser = { feedItemUrlInfo ->
            runCatching { uriHandler.openUri(feedItemUrlInfo.url.sanitizeUrl()) }
                .onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            strings.browserLaunchError,
                            duration = SnackbarDuration.Short,
                        )
                    }
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
    )

    val feedManagementActions = FeedManagementActions(
        onAddFeedClick = onAddFeedClick,
        onFeedFilterSelected = { feedFilter ->
            resetReaderArticle()
            homeViewModel.onFeedFilterSelected(feedFilter)
        },
        onEditFeedClick = onEditFeedClick,
        onDeleteFeedSourceClick = { feedSource -> homeViewModel.deleteFeedSource(feedSource) },
        onPinFeedClick = { feedSource -> homeViewModel.toggleFeedPin(feedSource) },
        onEditCategoryClick = { categoryId, newName -> homeViewModel.updateCategoryName(categoryId, newName) },
        onDeleteCategoryClick = { categoryId -> homeViewModel.deleteCategory(categoryId) },
        onChangeFeedCategoryClick = { feedSource ->
            changeFeedCategoryViewModel.loadFeedSource(feedSource)
            showChangeCategorySheet = true
        },
        onOpenWebsite = { url ->
            runCatching { uriHandler.openUri(url.sanitizeUrl()) }
                .onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            strings.browserLaunchError,
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
        },
        onMoveFeedSourcesToCategory = { feedSources, category ->
            changeFeedCategoryViewModel.moveFeedSourcesToCategory(feedSources, category)
        },
        onDeleteAllFeedsInCategoryClick = { feedSources ->
            homeViewModel.deleteAllFeedsInCategory(feedSources)
        },
    )

    val linkCopiedSuccess = LocalFeedFlowStrings.current.linkCopiedSuccess
    val shareBehavior = ShareBehavior(
        onShareClick = { urlTitle ->
            copyToClipboard(urlTitle.url)
            scope.launch {
                snackbarHostState.showSnackbar(message = linkCopiedSuccess)
            }
        },
        shareLinkTitle = LocalFeedFlowStrings.current.menuCopyLink,
        shareCommentsTitle = LocalFeedFlowStrings.current.menuCopyLinkComments,
    )
    if (isMultiPaneLayoutEnabled) {
        DesktopHomeScaffold(
            listState = listState,
            onSearchClick = onSearchClick,
            displayState = homeDisplayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            snackbarHostState = snackbarHostState,
            shareBehavior = shareBehavior,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            currentReaderArticle = currentReaderArticle,
            onReaderClosed = { readerModeViewModel.resetState() },
            onEmptyStateClick = {
                showNoFeedsBottomSheet = true
            },
        )
    } else {
        DesktopSinglePaneHomeScaffold(
            listState = listState,
            onSearchClick = onSearchClick,
            displayState = homeDisplayState,
            feedListActions = feedListActions,
            feedManagementActions = feedManagementActions,
            snackbarHostState = snackbarHostState,
            shareBehavior = shareBehavior,
            onFeedSuggestionsClick = onFeedSuggestionsClick,
            onEmptyStateClick = {
                showNoFeedsBottomSheet = true
            },
        )
    }

    if (showChangeCategorySheet) {
        EditCategoryDialog(
            categoryState = categoriesState,
            onCategorySelected = { categoryId ->
                changeFeedCategoryViewModel.onCategorySelected(categoryId)
            },
            onAddCategory = { categoryName ->
                changeFeedCategoryViewModel.addNewCategory(categoryName)
            },
            onDeleteCategory = { categoryId ->
                changeFeedCategoryViewModel.deleteCategory(categoryId.value)
            },
            onEditCategory = { categoryId, newName ->
                changeFeedCategoryViewModel.editCategory(categoryId, newName)
            },
            onDismiss = {
                changeFeedCategoryViewModel.saveCategory()
                showChangeCategorySheet = false
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
        runCatching { uriHandler.openUri(url) }
            .onFailure { onBrowserError() }
    }

    when (feedItemUrlInfo.linkOpeningPreference) {
        LinkOpeningPreference.READER_MODE -> {
            onOpenReaderArticle(feedItemUrlInfo)
        }

        LinkOpeningPreference.INTERNAL_BROWSER -> {
            openUri(feedItemUrlInfo.url)
        }

        LinkOpeningPreference.PREFERRED_BROWSER -> {
            openUri(feedItemUrlInfo.url)
        }

        LinkOpeningPreference.DEFAULT -> {
            if (browserManager.openReaderMode() && !feedItemUrlInfo.shouldOpenInBrowser()) {
                onOpenReaderArticle(feedItemUrlInfo)
            } else {
                openUri(feedItemUrlInfo.url.sanitizeUrl())
            }
        }
    }
}

private fun NextFeedPreviewState.asDisplayState(): NextFeedDisplayState = when (this) {
    is NextFeedPreviewDisabledState -> NextFeedDisplayState.NextFeedDisplayDisabledState
    is NextFeedPreviewEnabledState -> NextFeedDisplayState.NextFeedDisplayEnabledState(this.title)
}
