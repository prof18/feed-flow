package com.prof18.feedflow.android.feedsourcelist

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedSourceListScreen(
    onAddFeedClick: () -> Unit,
    onEditFeedClick: (feedSource: FeedSource) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<FeedSourceListViewModel>()
    val feedSources by viewModel.feedSourcesState.collectAsStateWithLifecycle()

    val strings = LocalFeedFlowStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val browserManager = koinInject<BrowserManager>()
    val context = LocalContext.current

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

    FeedSourceListContent(
        modifier = modifier,
        feedSourceListState = feedSources,
        onAddFeedClick = onAddFeedClick,
        onDeleteFeedClick = { feedSource ->
            viewModel.deleteFeedSource(feedSource)
        },
        onExpandClicked = { categoryId ->
            viewModel.expandCategory(categoryId)
        },
        navigateBack = navigateBack,
        onRenameFeedSourceClick = { feedSource, newName ->
            viewModel.updateFeedName(feedSource, newName)
        },
        onEditFeedSourceClick = onEditFeedClick,
        onPinFeedClick = { feedSource ->
            viewModel.toggleFeedPin(feedSource)
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        onOpenWebsite = { url -> browserManager.openUrlWithFavoriteBrowser(url, context) },
    )
}
