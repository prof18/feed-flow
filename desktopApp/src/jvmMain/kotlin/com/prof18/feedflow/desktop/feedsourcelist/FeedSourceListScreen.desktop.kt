package com.prof18.feedflow.desktop.feedsourcelist

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogWindow
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.desktop.addfeed.AddFeedScreen
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.preview.feedSourcesState
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceListContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf

@Composable
fun FeedSourceListScreen(
    navigateBack: () -> Unit,
) {
    var dialogState by remember { mutableStateOf(false) }

    DialogWindow(
        title = LocalFeedFlowStrings.current.addFeed,
        visible = dialogState,
        onCloseRequest = { dialogState = false },
    ) {
        AddFeedScreen(
            onFeedAdded = {
                dialogState = false
            },
        )
    }
    val viewModel = desktopViewModel { DI.koin.get<FeedSourceListViewModel>() }

    val feedSources by viewModel.feedSourcesState.collectAsState()

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
        navigateBack = navigateBack,
        onRenameFeedSourceClick = { feedSource, newName ->
            viewModel.updateFeedName(feedSource, newName)
        },
    )
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
            navigateBack = {},
            onRenameFeedSourceClick = { _, _ -> },
        )
    }
}
