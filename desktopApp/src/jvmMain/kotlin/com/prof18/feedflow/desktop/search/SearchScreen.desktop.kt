package com.prof18.feedflow.desktop.search

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.openInBrowser
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme

internal data class SearchScreen(
    private val viewModel: SearchViewModel,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val browserManager = DI.koin.get<BrowserManager>()

        val state: SearchState by viewModel.searchState.collectAsState()
        val searchQuery by viewModel.searchQueryState.collectAsState()

        SearchScreenContent(
            searchState = state,
            searchQuery = searchQuery,
            updateSearchQuery = { query ->
                viewModel.updateSearchQuery(query)
            },
            navigateBack = {
                viewModel.updateSearchQuery("")
                navigator.pop()
            },
            onFeedItemClick = { urlInfo ->
                if (browserManager.openReaderMode()) {
                    navigator.push(ReaderModeScreen(urlInfo))
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
