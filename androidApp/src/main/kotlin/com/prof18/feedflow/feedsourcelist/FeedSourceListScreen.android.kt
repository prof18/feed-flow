package com.prof18.feedflow.feedsourcelist

import FeedFlowTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
            FeedSourcesList(
                modifier = Modifier
                    .padding(paddingValues),
                feedSources = feedSources,
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
