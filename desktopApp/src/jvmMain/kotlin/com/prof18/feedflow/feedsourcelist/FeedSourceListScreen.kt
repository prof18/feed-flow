package com.prof18.feedflow.feedsourcelist

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.MR
import com.prof18.feedflow.addfeed.AddFeedScreen
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktopViewModel
import com.prof18.feedflow.di.DI
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.preview.feedSourcesForPreview
import com.prof18.feedflow.ui.feedsourcelist.FeedSourceNavBar
import com.prof18.feedflow.ui.feedsourcelist.FeedSourcesList
import com.prof18.feedflow.ui.feedsourcelist.NoFeedSourcesView
import com.prof18.feedflow.ui.style.FeedFlowTheme
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun FeedSourceListScreen(
    navigateBack: () -> Unit,
) {
    FeedFlowTheme {
        var dialogState by remember { mutableStateOf(false) }

        Dialog(
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
            feedSources = feedSources,
            onAddFeedClick = {
                dialogState = true
            },
            onDeleteFeedClick = { feedSource ->
                viewModel.deleteFeedSource(feedSource)
            },
            navigateBack = navigateBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FeedSourceListContent(
    feedSources: List<FeedSource>,
    onAddFeedClick: () -> Unit,
    onDeleteFeedClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            FeedSourceNavBar(
                navigateBack = navigateBack,
                onAddFeedSourceClick = onAddFeedClick,
            )
        },
    ) { paddingValues ->
        if (feedSources.isEmpty()) {
            NoFeedSourcesView(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        } else {
            val interactionSource = remember { MutableInteractionSource() }

            var showFeedMenu by remember {
                mutableStateOf(
                    false,
                )
            }

            FeedSourcesList(
                modifier = Modifier
                    .padding(paddingValues),
                feedSourceItemClickModifier = Modifier
                    .onClick(
                        enabled = true,
                        interactionSource = interactionSource,
                        matcher = PointerMatcher.mouse(PointerButton.Secondary), // Right Mouse Button
                        onClick = {
                            showFeedMenu = true
                        },
                    ),
                feedSources = feedSources,
                showFeedMenu = showFeedMenu,
                hideFeedMenu = {
                    showFeedMenu = false
                },
                onDeleteFeedSourceClick = onDeleteFeedClick,
            )
        }
    }
}

@Preview
@Composable
private fun FeedSourceListContentPreview() {
    FeedFlowTheme {
        FeedSourceListContent(
            feedSources = feedSourcesForPreview,
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            navigateBack = {},
        )
    }
}

@Preview
@Composable
private fun FeedSourceListContentDarkPreview() {
    FeedFlowTheme(
        darkTheme = true,
    ) {
        FeedSourceListContent(
            feedSources = feedSourcesForPreview,
            onAddFeedClick = {},
            onDeleteFeedClick = {},
            navigateBack = {},
        )
    }
}
