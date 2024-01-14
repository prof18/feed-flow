package com.prof18.feedflow.desktop.feedsourcelist

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.desktop.addfeed.AddFeedScreen
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.preview.feedSourcesState
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceListContent
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun FeedSourceListScreen(
    navigateBack: () -> Unit,
) {
    var dialogState by remember { mutableStateOf(false) }

    DialogWindow(
        title = stringResource(MR.strings.add_feed),
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
        feedSourceLogoImage = { imageUrl ->
            FeedSourceLogoImage(
                size = 24.dp,
                imageUrl = imageUrl,
            )
        },
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
    )
}

@Preview
@Composable
private fun FeedSourceListContentPreview() {
    FeedFlowTheme {
        FeedSourceListContent(
            feedSourceListState = FeedSourceListState(
                feedSourcesWithoutCategory = emptyList(),
                feedSourcesWithCategory = feedSourcesState,
            ),
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            onExpandClicked = {},
            navigateBack = {},
            feedSourceLogoImage = {
                FeedSourceLogoImage(
                    size = 24.dp,
                    imageUrl = it,
                )
            },
        )
    }
}
