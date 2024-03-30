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
    feedSourceLogoImage: @Composable (String) -> Unit,
    onAddFeedClick: () -> Unit,
    onDeleteFeedClick: (FeedSource) -> Unit,
    navigateBack: () -> Unit,
    onExpandClicked: (CategoryId?) -> Unit,
    onRenameFeedSourceClick: (FeedSource, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
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
                modifier = Modifier
                    .padding(paddingValues),
                feedSourceState = feedSourceListState,
                onExpandClicked = onExpandClicked,
                feedSourceImage = feedSourceLogoImage,
                onDeleteFeedSourceClick = onDeleteFeedClick,
                onRenameFeedSourceClick = onRenameFeedSourceClick,
            )
        }
    }
}
