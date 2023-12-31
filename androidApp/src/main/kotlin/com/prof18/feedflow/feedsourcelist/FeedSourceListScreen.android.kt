package com.prof18.feedflow.feedsourcelist

import FeedFlowTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.presentation.FeedSourceListViewModel
import com.prof18.feedflow.presentation.preview.feedSourcesState
import com.prof18.feedflow.ui.components.FeedSourceLogoImage
import com.prof18.feedflow.ui.feedsourcelist.FeedSourceNavBar
import com.prof18.feedflow.ui.feedsourcelist.FeedSourcesWithCategoryList
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
        onExpandClicked = { categoryId ->
            viewModel.expandCategory(categoryId)
        },
        navigateBack = navigateBack,
    )
}

@Composable
private fun FeedSourceListContent(
    feedSources: FeedSourceListState,
    onAddFeedSourceClick: () -> Unit,
    onDeleteFeedSourceClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
    onExpandClicked: (CategoryId?) -> Unit,
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
            FeedSourcesWithCategoryList(
                modifier = Modifier
                    .padding(paddingValues),
                feedSourceState = feedSources,
                feedSourceImage = { imageUrl ->
                    FeedSourceLogoImage(
                        size = 24.dp,
                        imageUrl = imageUrl,
                    )
                },
                onExpandClicked = onExpandClicked,
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
            feedSources = FeedSourceListState(
                feedSourcesWithoutCategory = emptyList(),
                feedSourcesWithCategory = feedSourcesState,
            ),
            onAddFeedSourceClick = { },
            onDeleteFeedSourceClick = {},
            navigateBack = {},
            onExpandClicked = {},
        )
    }
}
