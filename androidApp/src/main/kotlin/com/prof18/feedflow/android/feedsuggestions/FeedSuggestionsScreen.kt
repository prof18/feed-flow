package com.prof18.feedflow.android.feedsuggestions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsContent
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FeedSuggestionsScreen(
    navigateBack: () -> Unit,
    viewModel: FeedSuggestionsViewModel = koinViewModel(),
) {
    val categories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryIdState.collectAsState()
    val feedStatesMap by viewModel.feedStatesMapState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()

    FeedSuggestionsContent(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        feedStatesMap = feedStatesMap,
        isLoading = isLoading,
        onCategorySelected = viewModel::selectCategory,
        onAddFeed = { feed, categoryName ->
            viewModel.addFeed(feed, categoryName)
        },
        onNavigateBack = navigateBack,
        modifier = Modifier.fillMaxSize(),
    )
}
