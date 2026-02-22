package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsContent
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FeedSuggestionsScreen(
    onCloseRequest: () -> Unit,
) {
    val viewModel = koinViewModel<FeedSuggestionsViewModel>()
    val suggestedCategories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsState()
    val feedStatesMap by viewModel.feedStatesMapState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()

    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.feedSuggestionsTitle,
        size = DpSize(840.dp, 720.dp),
        onCloseRequest = onCloseRequest,
    ) { modifier ->
        FeedSuggestionsContent(
            categories = suggestedCategories,
            selectedCategoryId = selectedCategoryId,
            feedStatesMap = feedStatesMap,
            isLoading = isLoading,
            onCategorySelected = viewModel::selectCategory,
            onAddFeed = { feed, categoryName ->
                viewModel.addFeed(feed, categoryName)
            },
            modifier = modifier,
            contentPadding = PaddingValues(top = Spacing.regular),
        )
    }
}
