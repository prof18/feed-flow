package com.prof18.feedflow.android.search

import FeedFlowTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SearchScreen(
    navigateBack: () -> Unit,
    navigateToReaderMode: (FeedItemUrlInfo) -> Unit,
) {
    val viewModel = koinViewModel<SearchViewModel>()
    val browserManager = koinInject<BrowserManager>()
    val state: SearchState by viewModel.searchState.collectAsStateWithLifecycle()
    val feedFontSizes by viewModel.feedFontSizeState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQueryState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    SearchScreenContent(
        searchState = state,
        searchQuery = searchQuery,
        feedFontSizes = feedFontSizes,
        updateSearchQuery = { query ->
            viewModel.updateSearchQuery(query)
        },
        navigateBack = navigateBack,
        onFeedItemClick = { urlInfo ->
            if (browserManager.openReaderMode() && !urlInfo.openOnlyOnBrowser) {
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
        onCommentClick = { urlInfo ->
            browserManager.openUrlWithFavoriteBrowser(urlInfo.url, context)
            viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
        },
    )
}

@PreviewPhone
@Composable
private fun Preview() {
    FeedFlowTheme {
        SearchScreenContent(
            searchState = SearchState.EmptyState,
            feedFontSizes = FeedFontSizes(),
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
