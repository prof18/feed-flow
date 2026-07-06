package com.prof18.feedflow.android.feedsourcelist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceListState
import com.prof18.feedflow.core.model.FeedSourceState
import com.prof18.feedflow.shared.presentation.preview.feedSourcesState
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import kotlinx.collections.immutable.persistentListOf

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
    onReorderCategories: (List<FeedSourceState>) -> Unit = {},
    onReorderFeedSources: (List<FeedSource>) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
) {
    var isEditMode by rememberSaveable { mutableStateOf(false) }

    val canReorderAnything = feedSourceListState.feedSourcesWithoutCategory.size > 1 ||
        feedSourceListState.feedSourcesWithCategory.size > 1 ||
        feedSourceListState.feedSourcesWithCategory.any { it.feedSources.size > 1 }
    val editModeActive = isEditMode && canReorderAnything

    Scaffold(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            FeedSourceNavBar(
                navigateBack = navigateBack,
                onAddFeedSourceClick = onAddFeedClick,
                isEditMode = editModeActive,
                onToggleEditMode = { isEditMode = !isEditMode },
                showEditToggle = canReorderAnything,
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
                onReorderCategories = onReorderCategories,
                onReorderFeedSources = onReorderFeedSources,
                isEditMode = editModeActive,
                paddingValues = paddingValues,
            )
        }
    }
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
            onEditFeedSourceClick = {},
            onPinFeedClick = {},
            onOpenWebsite = {},
        )
    }
}
