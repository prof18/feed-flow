package com.prof18.feedflow.desktop.search

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.copyToClipboard
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SearchScreen(
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    navigateToEditFeed: (FeedSource) -> Unit,
) {
    val browserManager = DI.koin.get<BrowserManager>()
    val viewModel = koinViewModel<SearchViewModel>()

    val state: SearchState by viewModel.searchState.collectAsState()
    val searchQuery by viewModel.searchQueryState.collectAsState()
    val searchFilter by viewModel.searchFilterState.collectAsState()
    val currentFeedFilter by viewModel.searchFeedFilterState.collectAsState()
    val feedFontSizes by viewModel.feedFontSizeState.collectAsState()
    val strings = LocalFeedFlowStrings.current
    val uriHandler = LocalUriHandler.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorState.collect { errorState ->
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

    SearchScreenContent(
        searchState = state,
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
            viewModel.updateSearchQuery("")
            navigateBack()
        },
        onFeedItemClick = { urlInfo ->
            if (browserManager.openReaderMode()) {
                navigateToReaderMode(urlInfo)
            } else {
                uriHandler.openUri(urlInfo.url)
            }
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
        },
        onBookmarkClick = { feedItemId, isBookmarked ->
            viewModel.onBookmarkClick(feedItemId, isBookmarked)
        },
        onReadStatusClick = { feedItemId, isRead ->
            viewModel.onReadStatusClick(feedItemId, isRead)
        },
        onMarkAllAboveAsRead = { feedItemId ->
            viewModel.markAllAboveAsRead(feedItemId)
        },
        onMarkAllBelowAsRead = { feedItemId ->
            viewModel.markAllBelowAsRead(feedItemId)
        },
        onCommentClick = { urlInfo ->
            uriHandler.openUri(urlInfo.url)
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        onShareClick = { titleAndUrl ->
            copyToClipboard(titleAndUrl.url)
        },
        onOpenFeedSettings = { feedSource ->
            navigateToEditFeed(feedSource)
        },
        onOpenFeedWebsite = { url ->
            uriHandler.openUri(url)
        },
    )
}

@Preview
@Composable
private fun Preview() {
    FeedFlowTheme {
        SearchScreenContent(
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
