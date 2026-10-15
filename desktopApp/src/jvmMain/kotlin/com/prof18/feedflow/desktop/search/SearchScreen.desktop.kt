package com.prof18.feedflow.desktop.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.isReaderMode
import com.prof18.feedflow.core.model.resolveArticleOpenMode
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopSearchScreenContent
import com.prof18.feedflow.desktop.ui.components.DesktopSnackbarHost
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.desktop.utils.openUriSafely
import com.prof18.feedflow.shared.data.DesktopHomeSettingsRepository
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.presentation.model.syncErrorMessage
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SearchScreen(
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    navigateToEditFeed: (FeedSource) -> Unit,
) {
    val browserManager = DI.koin.get<BrowserManager>()
    val desktopHomeSettingsRepository = remember { DI.koin.get<DesktopHomeSettingsRepository>() }
    val viewModel = koinViewModel<SearchViewModel>()

    val state: SearchState by viewModel.searchState.collectAsState()
    val requestState by viewModel.searchRequestState.collectAsState()
    val searchQuery by viewModel.searchQueryState.collectAsState()
    val searchFilter by viewModel.searchFilterState.collectAsState()
    val currentFeedFilter by viewModel.searchFeedFilterState.collectAsState()
    val feedFontSizes by viewModel.feedFontSizeState.collectAsState()
    val feedItemDisplaySettings by viewModel.feedItemDisplaySettings.collectAsState()
    val isMultiPaneLayoutEnabled by desktopHomeSettingsRepository.isMultiPaneLayoutEnabledFlow.collectAsState()
    val strings = LocalFeedFlowStrings.current
    val uriHandler = LocalUriHandler.current

    val snackbarHostState = remember { DesktopFeedbackState() }
    val scope = rememberCoroutineScope()
    fun openExternalUrl(url: String): Boolean {
        val opened = uriHandler.openUriSafely(url)
        if (!opened) {
            scope.launch {
                snackbarHostState.showFeedback(
                    message = strings.browserLaunchError,
                    duration = DesktopFeedbackDuration.Short,
                )
            }
        }
        return opened
    }

    LaunchedEffect(Unit) {
        viewModel.errorState.collect { errorState ->
            when (errorState) {
                is UIErrorState.DatabaseError -> {
                    snackbarHostState.showFeedback(
                        message = strings.databaseError(errorState.errorCode.code),
                        duration = DesktopFeedbackDuration.Short,
                    )
                }

                is UIErrorState.SyncError -> {
                    snackbarHostState.showFeedback(
                        message = strings.syncErrorMessage(errorState.errorCode),
                        duration = DesktopFeedbackDuration.Short,
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

    DesktopSearchScreenContent(
        searchState = state,
        searchRequestState = requestState,
        onRetrySearch = viewModel::retrySearch,
        searchQuery = searchQuery,
        searchFilter = searchFilter,
        currentFeedFilter = currentFeedFilter,
        feedFontSizes = feedFontSizes,
        shareMenuLabel = strings.menuCopyLink,
        shareCommentsMenuLabel = strings.menuCopyLinkComments,
        updateSearchQuery = { query ->
            viewModel.updateSearchQuery(query)
        },
        onSearchFilterSelected = { filter ->
            viewModel.updateSearchFilter(filter)
        },
        navigateBack = {
            viewModel.resetSearch()
            navigateBack()
        },
        onFeedItemClick = { urlInfo ->
            if (viewModel.isCurrentSearchResult(urlInfo.id)) {
                openSearchResult(
                    urlInfo = urlInfo,
                    browserManager = browserManager,
                    openUri = ::openExternalUrl,
                    navigateToReaderMode = navigateToReaderMode,
                )
                viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
            }
        },
        onBookmarkClick = { feedItemId, isBookmarked ->
            if (viewModel.isCurrentSearchResult(feedItemId.id)) {
                viewModel.onBookmarkClick(feedItemId, isBookmarked)
            }
        },
        onReadStatusClick = { feedItemId, isRead ->
            if (viewModel.isCurrentSearchResult(feedItemId.id)) {
                viewModel.onReadStatusClick(feedItemId, isRead)
            }
        },
        onMarkAllAboveAsRead = { feedItemId ->
            if (viewModel.isCurrentSearchResult(feedItemId)) viewModel.markAllAboveAsRead(feedItemId)
        },
        onMarkAllBelowAsRead = { feedItemId ->
            if (viewModel.isCurrentSearchResult(feedItemId)) viewModel.markAllBelowAsRead(feedItemId)
        },
        onCommentClick = { urlInfo ->
            if (viewModel.isCurrentSearchResult(urlInfo.id)) {
                openExternalUrl(urlInfo.url)
                viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
            }
        },
        snackbarHost = {
            DesktopSnackbarHost(snackbarHostState)
        },
        onShareClick = { titleAndUrl ->
            copyToClipboard(titleAndUrl.url)
        },
        onOpenFeedSettings = { feedSource ->
            navigateToEditFeed(feedSource)
        },
        onOpenFeedWebsite = ::openExternalUrl,
        isGridLayoutEnabled = !isMultiPaneLayoutEnabled,
        feedItemDisplaySettings = feedItemDisplaySettings,
    )
}

private fun openSearchResult(
    urlInfo: FeedItemUrlInfo,
    browserManager: BrowserManager,
    openUri: (String) -> Boolean,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val openMode = urlInfo.resolveArticleOpenMode(browserManager.getArticleOpenMode())
    if (openMode.isReaderMode()) {
        navigateToReaderMode(urlInfo)
    } else {
        openUri(urlInfo.url)
    }
}

@Preview
@Composable
private fun Preview() {
    FeedFlowTheme {
        DesktopSearchScreenContent(
            searchState = SearchState.EmptyState,
            searchQuery = "",
            searchFilter = SearchFilter.All,
            feedFontSizes = FeedFontSizes(),
            shareMenuLabel = "Share",
            shareCommentsMenuLabel = "Share comments",
            updateSearchQuery = {},
            onSearchFilterSelected = {},
            navigateBack = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onMarkAllAboveAsRead = { _ -> },
            onMarkAllBelowAsRead = { _ -> },
            onCommentClick = {},
            onShareClick = { _ -> },
            onOpenFeedSettings = {},
            onOpenFeedWebsite = {},
            currentFeedFilter = null,
        )
    }
}
