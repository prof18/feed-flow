package com.prof18.feedflow.android.feedsourcelist

import FeedFlowTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.shared.presentation.FeedSourceListViewModel
import com.prof18.feedflow.shared.presentation.preview.feedSourcesState
import com.prof18.feedflow.shared.ui.feedsourcelist.FeedSourceListContent
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

@Composable
fun FeedSourceListScreen(
    onAddFeedClick: () -> Unit,
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<FeedSourceListViewModel>()
    val feedSources by viewModel.feedSourcesState.collectAsStateWithLifecycle()

    FeedSourceListContent(
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
    )
}

@PreviewPhone
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
