package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.categoryselection.EditCategoryDialog
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.sanitizeUrl
import com.prof18.feedflow.shared.presentation.ChangeFeedCategoryViewModel
import com.prof18.feedflow.shared.presentation.HomeViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.home.AdaptiveHomeView
import com.prof18.feedflow.shared.ui.home.FeedListActions
import com.prof18.feedflow.shared.ui.home.FeedManagementActions
import com.prof18.feedflow.shared.ui.home.HomeDisplayState
import com.prof18.feedflow.shared.ui.home.ShareBehavior
import com.prof18.feedflow.shared.ui.home.WindowSizeClass
import com.prof18.feedflow.shared.ui.home.components.LoadingOperationDialog
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    homeViewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    onImportExportClick: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onSettingsButtonClicked: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val changeFeedCategoryViewModel = desktopViewModel { DI.koin.get<ChangeFeedCategoryViewModel>() }

    val loadingState by homeViewModel.loadingState.collectAsState()
    val feedState by homeViewModel.feedState.collectAsState()
    val navDrawerState by homeViewModel.navDrawerState.collectAsState()
    val currentFeedFilter by homeViewModel.currentFeedFilter.collectAsState()
    val unReadCount by homeViewModel.unreadCountFlow.collectAsState(initial = 0)
    val feedFontSizes by homeViewModel.feedFontSizeState.collectAsState()
    val swipeActions by homeViewModel.swipeActions.collectAsState()
    val feedOperation by homeViewModel.feedOperationState.collectAsState()
    val feedLayout by homeViewModel.feedLayout.collectAsState()

    val categoriesState by changeFeedCategoryViewModel.categoriesState.collectAsState()

    var showChangeCategorySheet by remember { mutableStateOf(false) }

    val browserManager = DI.koin.get<BrowserManager>()
    val strings = LocalFeedFlowStrings.current
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.currentOrThrow

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

    var showDialog by remember { mutableStateOf(false) }
    NoFeedsDialog(
        showDialog = showDialog,
        onDismissRequest = {
            showDialog = false
        },
        onImportExportClick = onImportExportClick,
        onAccountsClick = onAccountsClick,
    )

    val homeDisplayState = HomeDisplayState(
        feedItems = feedState,
        navDrawerState = navDrawerState,
        unReadCount = unReadCount,
        feedUpdateStatus = loadingState,
        feedFontSizes = feedFontSizes,
        currentFeedFilter = currentFeedFilter,
        swipeActions = swipeActions,
        feedLayout = feedLayout,
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
        openUrl = { feedItemUrlInfo ->
            when (feedItemUrlInfo.linkOpeningPreference) {
                LinkOpeningPreference.READER_MODE -> navigateToReaderMode(feedItemUrlInfo)
                LinkOpeningPreference.INTERNAL_BROWSER -> uriHandler.openUri(feedItemUrlInfo.url)
                LinkOpeningPreference.PREFERRED_BROWSER -> uriHandler.openUri(feedItemUrlInfo.url)
                LinkOpeningPreference.DEFAULT -> {
                    if (browserManager.openReaderMode() && !feedItemUrlInfo.shouldOpenInBrowser()) {
                        navigateToReaderMode(feedItemUrlInfo)
                    } else {
                        uriHandler.openUri(feedItemUrlInfo.url.sanitizeUrl())
                    }
                }
            }
        },
        updateBookmarkStatus = { feedItemId, isBookmarked ->
            homeViewModel.updateBookmarkStatus(feedItemId, isBookmarked)
        },
        updateReadStatus = { feedItemId, isRead -> homeViewModel.updateReadStatus(feedItemId, isRead) },
        markAllAboveAsRead = { feedItemId -> homeViewModel.markAllAboveAsRead(feedItemId) },
        markAllBelowAsRead = { feedItemId -> homeViewModel.markAllBelowAsRead(feedItemId) },
    )

    val feedManagementActions = FeedManagementActions(
        onAddFeedClick = { showDialog = true },
        onFeedFilterSelected = { feedFilter -> homeViewModel.onFeedFilterSelected(feedFilter) },
        onEditFeedClick = { feedSource -> navigator.push(EditFeedScreen(feedSource)) }, // Pass the navigator action
        onDeleteFeedSourceClick = { feedSource -> homeViewModel.deleteFeedSource(feedSource) },
        onPinFeedClick = { feedSource -> homeViewModel.toggleFeedPin(feedSource) },
        onEditCategoryClick = { categoryId, newName -> homeViewModel.updateCategoryName(categoryId, newName) },
        onDeleteCategoryClick = { categoryId -> homeViewModel.deleteCategory(categoryId) },
        onChangeFeedCategoryClick = { feedSource ->
            changeFeedCategoryViewModel.loadFeedSource(feedSource)
            showChangeCategorySheet = true
        },
        onOpenWebsite = { url -> uriHandler.openUri(url.sanitizeUrl()) },
    )

    val linkCopiedSuccess = LocalFeedFlowStrings.current.linkCopiedSuccess
    AdaptiveHomeView(
        listState = listState,
        onSearchClick = onSearchClick,
        onSettingsButtonClicked = onSettingsButtonClicked,
        displayState = homeDisplayState,
        feedListActions = feedListActions,
        feedManagementActions = feedManagementActions,
        snackbarHostState = snackbarHostState,
        shareBehavior = ShareBehavior(
            onShareClick = { urlTitle ->
                copyToClipboard(urlTitle.url)
                scope.launch {
                    snackbarHostState.showSnackbar(message = linkCopiedSuccess)
                }
            },
            shareLinkTitle = LocalFeedFlowStrings.current.menuCopyLink,
            shareCommentsTitle = LocalFeedFlowStrings.current.menuCopyLinkComments,
        ),
        windowSizeClass = when (calculateWindowSizeClass().widthSizeClass) {
            WindowWidthSizeClass.Compact -> WindowSizeClass.Compact
            WindowWidthSizeClass.Medium -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        },
        showDropdownMenu = false,
        feedContentWrapper = { content ->
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = Spacing.xsmall),
                ) {
                    content()
                }

                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(
                        scrollState = listState,
                    ),
                )
            }
        },
    )

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
}
