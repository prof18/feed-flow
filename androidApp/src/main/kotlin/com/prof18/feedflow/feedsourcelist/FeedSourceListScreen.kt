@file:OptIn(ExperimentalMaterial3Api::class)

package com.prof18.feedflow.feedsourcelist

import FeedFlowTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.preview.feedSourcesForPreview
import com.prof18.feedflow.ui.feedsourcelist.FeedSourceNavBar
import com.prof18.feedflow.ui.feedsourcelist.FeedSourcesList
import com.prof18.feedflow.ui.feedsourcelist.NoFeedSourcesView
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import org.koin.androidx.compose.koinViewModel

@Composable
fun FeedSourceListScreen(
    onAddFeedClick: () -> Unit,
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<FeedSourceListViewModel>()
    val feedSources by viewModel.feedSourcesState.collectAsStateWithLifecycle()

    FeedSourceListContent(
        feedSources = feedSources,
        onAddFeedSourceClick = onAddFeedClick,
        onDeleteFeedSourceClick = { feedSource ->
            viewModel.deleteFeedSource(feedSource)
        },
        navigateBack = navigateBack,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedSourceListContent(
    feedSources: List<FeedSource>,
    onAddFeedSourceClick: () -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            FeedSourceNavBar(
                navigateBack = navigateBack,
                onAddFeedSourceClick = onAddFeedSourceClick,
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
            var showFeedMenu by remember {
                mutableStateOf(
                    false,
                )
            }

            FeedSourcesList(
                modifier = Modifier
                    .padding(paddingValues),
                feedSourceItemClickModifier = Modifier
                    .combinedClickable(
                        onClick = {
                            // TODO: open edit feed
                        },
                        onLongClick = {
                            showFeedMenu = true
                        },
                    ),
                feedSources = feedSources,
                showFeedMenu = showFeedMenu,
                hideFeedMenu = {
                    showFeedMenu = false
                },
                onDeleteFeedSourceClick = onDeleteFeedSourceClick,
            )
        }
    }
}

@FeedFlowPreview
@Composable
private fun FeedSourceListContentPreview() {
    FeedFlowTheme {
        FeedSourceListContent(
            feedSources = feedSourcesForPreview,
            onAddFeedSourceClick = { },
            onDeleteFeedSourceClick = {},
            navigateBack = {},
        )
    }
}
