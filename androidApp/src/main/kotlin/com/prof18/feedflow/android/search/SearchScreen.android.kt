package com.prof18.feedflow.android.search

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.shouldOpenInBrowser
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SearchScreen(
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
    navigateToEditFeed: (com.prof18.feedflow.core.model.FeedSource) -> Unit,
) {
    val viewModel = koinViewModel<SearchViewModel>()
    val browserManager = koinInject<BrowserManager>()
    val state: SearchState by viewModel.searchState.collectAsStateWithLifecycle()
    val feedFontSizes by viewModel.feedFontSizeState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQueryState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val strings = LocalFeedFlowStrings.current

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
        feedFontSizes = feedFontSizes,
        shareMenuLabel = strings.menuShare,
        shareCommentsMenuLabel = strings.menuShareComments,
        updateSearchQuery = { query ->
            viewModel.updateSearchQuery(query)
        },
        navigateBack = navigateBack,
        onFeedItemClick = { urlInfo ->
            if (browserManager.openReaderMode() && !urlInfo.shouldOpenInBrowser()) {
                navigateToReaderMode(urlInfo)
            } else {
                browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
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
            browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        onShareClick = { titleAndUrl ->
            context.openShareSheet(
                title = titleAndUrl.title,
                url = titleAndUrl.url,
            )
        },
        onOpenFeedSettings = navigateToEditFeed,
    )
}

@PreviewPhone
@Composable
private fun Preview() {
    FeedFlowTheme {
        SearchScreenContent(
            searchState = SearchState.EmptyState,
            feedFontSizes = FeedFontSizes(),
            shareCommentsMenuLabel = "Share Comments",
            shareMenuLabel = "Share",
            searchQuery = "",
            updateSearchQuery = {},
            navigateBack = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onMarkAllAboveAsRead = { _ -> },
            onMarkAllBelowAsRead = { _ -> },
            onCommentClick = {},
            onShareClick = { _ -> },
            onOpenFeedSettings = {},
        )
    }
}
