package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsContent
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun FeedSuggestionsScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<FeedSuggestionsViewModel>()
    val suggestedCategories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsState()
    val feedStatesMap by viewModel.feedStatesMapState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()

    FeedSuggestionsContent(
        categories = suggestedCategories.toPersistentList(),
        selectedCategoryId = selectedCategoryId,
        feedStatesMap = feedStatesMap,
        isLoading = isLoading,
        onCategorySelected = viewModel::selectCategory,
        onAddFeed = { feed, categoryName ->
            viewModel.addFeed(feed, categoryName)
        },
        onNavigateBack = navigateBack,
    )
}
