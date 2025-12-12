package com.prof18.feedflow.desktop.feedsourcelist

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.window.DialogWindow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.desktop.addfeed.AddFeedScreenContent
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.editfeed.EditFeedScreen
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import com.prof18.feedflow.shared.presentation.preview.feedSourcesState
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceListContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

class FeedSourceListScreen : Screen {

    @Composable
    override fun Content() {
        var dialogState by remember { mutableStateOf(false) }

        DialogWindow(
            title = LocalFeedFlowStrings.current.addFeed,
            visible = dialogState,
            onCloseRequest = { dialogState = false },
        ) {
            AddFeedScreenContent(
                onFeedAdded = {
                    dialogState = false
                },
            )
        }
        val viewModel = desktopViewModel { DI.koin.get<FeedSourceListViewModel>() }
        val feedSources by viewModel.feedSourcesState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val strings = LocalFeedFlowStrings.current
        val snackbarHostState = remember { SnackbarHostState() }
        val uriHandler = LocalUriHandler.current

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
                            strings.syncErrorMessage((errorState.errorCode.code)),
                            duration = SnackbarDuration.Short,
                        )
                    }
                }
            }
        }

        FeedSourceListContent(
            feedSourceListState = feedSources,
            onAddFeedClick = {
                dialogState = true
            },
            onDeleteFeedClick = { feedSource ->
                viewModel.deleteFeedSource(feedSource)
            },
            onExpandClicked = { categoryId ->
                viewModel.expandCategory(categoryId)
            },
            navigateBack = {
                navigator.pop()
            },
            onEditFeedSourceClick = { feedSource ->
                navigator.push(EditFeedScreen(feedSource))
            },
            onRenameFeedSourceClick = { feedSource, newName ->
                viewModel.updateFeedName(feedSource, newName)
            },
            onPinFeedClick = { feedSource ->
                viewModel.toggleFeedPin(feedSource)
            },
            snackbarHost = {
                SnackbarHost(snackbarHostState)
            },
            onOpenWebsite = { url -> uriHandler.openUri(url) },
        )
    }
}

@Preview
@Composable
private fun FeedSourceListContentPreview() {
    FeedFlowTheme {
        FeedSourceListContent(
            feedSourceListState = FeedSourceListState(
                feedSourcesWithoutCategory = persistentListOf(),
                feedSourcesWithCategory = feedSourcesState,
            ),
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            onExpandClicked = {},
            onEditFeedSourceClick = {},
            navigateBack = {},
            onRenameFeedSourceClick = { _, _ -> },
            onPinFeedClick = {},
            onOpenWebsite = {},
        )
    }
}
