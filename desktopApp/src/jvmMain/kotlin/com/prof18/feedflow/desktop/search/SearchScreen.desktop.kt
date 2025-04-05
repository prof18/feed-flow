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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.BrowserManager
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.reaadermode.ReaderModeScreen
import com.prof18.feedflow.shared.presentation.SearchViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.search.SearchScreenContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal data class SearchScreen(
    private val viewModel: SearchViewModel,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val browserManager = DI.koin.get<BrowserManager>()

        val state: SearchState by viewModel.searchState.collectAsState()
        val searchQuery by viewModel.searchQueryState.collectAsState()
        val feedFontSizes by viewModel.feedFontSizeState.collectAsState()
        val strings = LocalFeedFlowStrings.current
        val uriHandler = LocalUriHandler.current

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            viewModel.errorState.collect { errorState ->
                when (errorState) {
                    UIErrorState.DatabaseError -> {
                        snackbarHostState.showSnackbar(
                            strings.databaseError,
                            duration = SnackbarDuration.Short,
                        )
                    }

                    is UIErrorState.FeedErrorState -> {
                        snackbarHostState.showSnackbar(
                            strings.feedErrorMessage(errorState.feedName),
                            duration = SnackbarDuration.Short,
                        )
                    }

                    UIErrorState.SyncError -> {
                        snackbarHostState.showSnackbar(
                            strings.syncErrorMessage,
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
            shareMenuLabel = strings.menuCopyLink,
            shareCommentsMenuLabel = strings.menuCopyLinkComments,
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
            onCommentClick = { urlInfo ->
                uriHandler.openUri(urlInfo.url)
                viewModel.onReadStatusClick(FeedItemId(urlInfo.id), true)
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            onShareClick = { titleAndUrl ->
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(titleAndUrl.url), null)
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
            feedFontSizes = FeedFontSizes(),
            shareMenuLabel = "Share",
            shareCommentsMenuLabel = "Share comments",
            updateSearchQuery = {},
            navigateBack = {},
            onFeedItemClick = {},
            onBookmarkClick = { _, _ -> },
            onReadStatusClick = { _, _ -> },
            onCommentClick = {},
            onShareClick = { _ -> },
        )
    }
}
