package com.prof18.feedflow.desktop.search

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme

@Composable
internal fun SearchScreen(
    viewModel: SearchViewModel,
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val browserManager = DI.koin.get<BrowserManager>()
    val state: SearchState by viewModel.searchState.collectAsState()

    val searchQuery by viewModel.searchQueryState.collectAsState()

    SearchScreenContent(
        searchState = state,
        searchQuery = searchQuery,
        updateSearchQuery = { query ->
            viewModel.updateSearchQuery(query)
        },
        navigateBack = navigateBack,
        onFeedItemClick = { urlInfo ->
            if (browserManager.openReaderMode()) {
                navigateToReaderMode(urlInfo)
            } else {
                openInBrowser(urlInfo.url)
            }
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
        },
        onBookmarkClick = { feedItemId, isBookmarked ->
            viewModel.onBookmarkClick(feedItemId, isBookmarked)
        },
        onReadStatusClick = { feedItemId, isRead ->
            viewModel.onReadStatusClick(feedItemId, isRead)
        },
        onCommentClick = { urlInfo ->
            openInBrowser(urlInfo.url)
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
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
            updateSearchQuery = {},
            navigateBack = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
        )
    }
}
