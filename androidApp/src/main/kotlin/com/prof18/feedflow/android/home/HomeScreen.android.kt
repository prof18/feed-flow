package com.prof18.feedflow.android.home

import android.content.Context
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.categoryselection.EditCategorySheet
import com.prof18.feedflow.android.home.drawer.AndroidDrawer
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.shared.data.AndroidHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.ReaderModeViewModel
import com.prof18.feedflow.shared.presentation.ThemeViewModel
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
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    readerModeViewModel: ReaderModeViewModel,
    onSettingsButtonClicked: () -> Unit,
    onAddFeedClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onEditFeedClick: (FeedSource) -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    onNavigateToNextFeed: () -> Unit,
    onImportExportClick: () -> Unit = {},
) {
    val browserManager = koinInject<BrowserManager>()
    val changeFeedCategoryViewModel: ChangeFeedCategoryViewModel = koinInject()
    val themeViewModel: ThemeViewModel = koinViewModel()

    val loadingState by homeViewModel.loadingState.collectAsStateWithLifecycle()
    val feedState by homeViewModel.feedState.collectAsStateWithLifecycle()
    val navDrawerState by homeViewModel.navDrawerState.collectAsStateWithLifecycle()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsStateWithLifecycle()
    val nextFeedPreviewState: NextFeedPreviewState by homeViewModel.nextFeedPreviewState.collectAsStateWithLifecycle()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsStateWithLifecycle(initialValue = 0)
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsStateWithLifecycle()
    val swipeActions by homeViewModel.swipeActions.collectAsStateWithLifecycle()
    val feedOperation by homeViewModel.feedOperationState.collectAsStateWithLifecycle()
    val feedLayout by homeViewModel.feedLayout.collectAsStateWithLifecycle()
    val isSyncUploadRequired by homeViewModel.isSyncUploadRequired.collectAsStateWithLifecycle()
    val feedItemDisplaySettings by homeViewModel.feedItemDisplaySettings.collectAsStateWithLifecycle()

    val categoriesState by changeFeedCategoryViewModel.categoriesState.collectAsStateWithLifecycle()
    val currentReaderArticle by readerModeViewModel.currentArticleState.collectAsStateWithLifecycle()
    val readerModeState by readerModeViewModel.readerModeState.collectAsStateWithLifecycle()
    val readerFontSize by readerModeViewModel.readerFontSizeState.collectAsStateWithLifecycle()
    val canNavigatePrevious by readerModeViewModel.canNavigateToPreviousState.collectAsStateWithLifecycle()
    val canNavigateNext by readerModeViewModel.canNavigateToNextState.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.themeState.collectAsStateWithLifecycle()

    var showChangeCategorySheet by rememberSaveable { mutableStateOf(false) }
    var showNoFeedsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val changeCategorySheetState = rememberModalBottomSheetState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val strings = LocalFeedFlowStrings.current

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
        isSyncUploadRequired = isSyncUploadRequired,
        nextFeedDisplayState = nextFeedPreviewState.asDisplayState(),
        feedItemDisplaySettings = feedItemDisplaySettings,
    )

    val openReaderArticle: (FeedItemUrlInfo) -> Unit = { article ->
        readerModeViewModel.getReaderModeHtml(article)
    }
    val closeReaderSelection: () -> Unit = {
        readerModeViewModel.clearSelection()
    }
    val resetReaderArticle: () -> Unit = {
        readerModeViewModel.resetState()
    }

    val feedListActions = FeedListActions(
        onClearOldArticlesClicked = { homeViewModel.deleteOldFeedItems() },
        onDeleteDatabaseClick = { homeViewModel.deleteAllFeeds() },
        refreshData = { homeViewModel.getNewFeeds() },
        requestNewData = { homeViewModel.requestNewFeedsPage() },
        forceRefreshData = { homeViewModel.forceFeedRefresh() },
        markAllRead = { homeViewModel.markAllRead() },
        onBackToTimelineClick = {
            resetReaderArticle()
            homeViewModel.onFeedFilterSelected(FeedFilter.Timeline)
        },
        markAsReadOnScroll = { lastVisibleIndex -> homeViewModel.markAsReadOnScroll(lastVisibleIndex) },
        markAsRead = { feedItemId -> homeViewModel.markAsRead(feedItemId.id) },
        openUrl = { urlInfo ->
            openUrl(
                urlInfo = urlInfo,
                onOpenReaderArticle = openReaderArticle,
                browserManager = browserManager,
                context = context,
            )
        },
        openInBrowser = { urlInfo -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context) },
        updateBookmarkStatus = { feedItemId, isBookmarked ->
            homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
        },
        updateReadStatus = { feedItemId, isRead -> homeViewModel.updateReadStatus(feedItemId, isRead) },
        markAllAboveAsRead = { feedItemId -> homeViewModel.markAllAboveAsRead(feedItemId) },
        markAllBelowAsRead = { feedItemId -> homeViewModel.markAllBelowAsRead(feedItemId) },
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
        onOpenWebsite = { url -> browserManager.openUrlWithFavoriteBrowser(url, context) },
    )

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val reduceMotionEnabled = LocalReduceMotion.current
    val homeSettingsRepository = koinInject<AndroidHomeSettingsRepository>()
    val isMultiPaneEnabled by homeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsStateWithLifecycle()

    val shareBehavior = ShareBehavior(
        onShareClick = { titleAndUrl ->
            context.openShareSheet(
                title = titleAndUrl.title,
                url = titleAndUrl.url,
            )
        },
        shareLinkTitle = strings.menuShare,
        shareCommentsTitle = strings.menuShareComments,
    )

    AndroidThreePaneHomeScaffold(
        currentReaderArticle = currentReaderArticle,
        readerModeState = readerModeState,
        readerFontSize = readerFontSize,
        themeMode = themeMode,
        canNavigatePrevious = canNavigatePrevious,
        canNavigateNext = canNavigateNext,
        isMultiPaneEnabled = isMultiPaneEnabled,
        initialPaneExpansionIndex = homeSettingsRepository.getPaneExpansionIndex(),
        onReaderClosed = closeReaderSelection,
        onUpdateReaderFontSize = readerModeViewModel::updateFontSize,
        onReaderBookmarkClick = readerModeViewModel::updateBookmarkStatus,
        onNavigateToPreviousArticle = readerModeViewModel::navigateToPreviousArticle,
        onNavigateToNextArticle = readerModeViewModel::navigateToNextArticle,
        onPaneExpansionIndexChanged = homeSettingsRepository::setPaneExpansionIndex,
        drawerPane = { modifier, _, onCloseDrawer ->
            AndroidDrawer(
                modifier = modifier,
                displayState = homeDisplayState,
                feedManagementActions = feedManagementActions,
                onFeedFilterSelected = { feedFilter ->
                    feedManagementActions.onFeedFilterSelected(feedFilter)
                    onCloseDrawer()
                    scope.launch {
                        listState.scrollToItemConditionally(
                            0,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                    }
                },
                onFeedSuggestionsClick = {
                    onCloseDrawer()
                    onFeedSuggestionsClick()
                },
            )
        },
        listPane = { modifier, isDrawerOpen, onDrawerClick ->
            AndroidHomeScreenContent(
                modifier = modifier,
                displayState = homeDisplayState,
                feedListActions = feedListActions,
                feedManagementActions = feedManagementActions,
                listState = listState,
                snackbarHostState = snackbarHostState,
                onSearchClick = onSearchClick,
                onSettingsButtonClicked = onSettingsButtonClicked,
                showDrawerMenu = true,
                isDrawerOpen = isDrawerOpen,
                onDrawerMenuClick = onDrawerClick,
                onRefresh = feedListActions.refreshData,
                shareBehavior = shareBehavior,
                onBackupClick = homeViewModel::enqueueBackup,
                onEmptyStateClick = { showNoFeedsBottomSheet = true },
                onNavigateToNextFeed = {
                    scope.launch {
                        listState.scrollToItemConditionally(
                            0,
                            reduceMotionEnabled = reduceMotionEnabled,
                        )
                    }
                    onNavigateToNextFeed()
                },
            )
        },
    )

    if (showChangeCategorySheet) {
        EditCategorySheet(
            sheetState = changeCategorySheetState,
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
            },
        )
    }

    if (showNoFeedsBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        NoFeedsBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                showNoFeedsBottomSheet = false
            },
            onAddFeedClick = {
                showNoFeedsBottomSheet = false
                onAddFeedClick()
            },
            onImportExportClick = {
                showNoFeedsBottomSheet = false
                onImportExportClick()
            },
            onAccountsClick = {
                showNoFeedsBottomSheet = false
                onAccountsClick()
            },
            onFeedSuggestionsClick = {
                showNoFeedsBottomSheet = false
                onFeedSuggestionsClick()
            },
        )
    }
}

private fun openUrl(
    urlInfo: FeedItemUrlInfo,
    onOpenReaderArticle: (FeedItemUrlInfo) -> Unit,
    browserManager: BrowserManager,
    context: Context,
) {
    when (urlInfo.linkOpeningPreference) {
        LinkOpeningPreference.READER_MODE -> onOpenReaderArticle(urlInfo)
        LinkOpeningPreference.INTERNAL_BROWSER -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
        LinkOpeningPreference.PREFERRED_BROWSER -> browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
        LinkOpeningPreference.DEFAULT -> {
            if (browserManager.openReaderMode() && !urlInfo.shouldOpenInBrowser()) {
                onOpenReaderArticle(urlInfo)
            } else {
                browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
            }
        }
    }
}

fun NextFeedPreviewState.asDisplayState(): NextFeedDisplayState = when (this) {
    is NextFeedPreviewDisabledState -> NextFeedDisplayState.NextFeedDisplayDisabledState
    is NextFeedPreviewEnabledState -> NextFeedDisplayState.NextFeedDisplayEnabledState(this.title)
}
