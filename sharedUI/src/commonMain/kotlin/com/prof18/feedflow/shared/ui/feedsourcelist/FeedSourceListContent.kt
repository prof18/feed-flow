package com.prof18.feedflow.shared.ui.feedsourcelist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceListState

@Composable
fun FeedSourceListContent(
    feedSourceListState: FeedSourceListState,
    onAddFeedClick: () -> Unit,
    onDeleteFeedClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
    onExpandClicked: (CategoryId?) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    onEditFeedSourceClick: (FeedSource) -> Unit,
    onPinFeedClick: (FeedSource) -> Unit,
    onOpenWebsite: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteAllFeedsInCategory: (List<FeedSource>) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            FeedSourceNavBar(
                navigateBack = navigateBack,
                onAddFeedSourceClick = onAddFeedClick,
            )
        },
    ) { paddingValues ->
        if (feedSourceListState.isEmpty()) {
            NoFeedSourcesView(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
            )
        } else {
            FeedSourcesWithCategoryList(
                feedSourceState = feedSourceListState,
                onExpandClicked = onExpandClicked,
                onDeleteFeedSourceClick = onDeleteFeedClick,
                onRenameFeedSourceClick = onRenameFeedSourceClick,
                onEditFeedClick = onEditFeedSourceClick,
                onPinFeedClick = onPinFeedClick,
                onOpenWebsite = onOpenWebsite,
                onDeleteAllFeedsInCategory = onDeleteAllFeedsInCategory,
                paddingValues = paddingValues,
            )
        }
    }
}
