package com.prof18.feedflow.android.feedsuggestions

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.presentation.OnboardingViewModel
import com.prof18.feedflow.shared.ui.onboarding.FeedSuggestionsContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun FeedSuggestionsScreen(
    navigateBack: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val categories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedFeeds by viewModel.selectedFeedsState.collectAsState()
    val expandedCategories by viewModel.expandedCategoriesState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()

    FeedSuggestionsContent(
        categories = categories,
        selectedFeeds = selectedFeeds,
        expandedCategories = expandedCategories,
        isLoading = isLoading,
        onFeedToggle = viewModel::toggleFeedSelection,
        onCategoryToggle = viewModel::toggleCategoryExpansion,
        onAddFeeds = {
            viewModel.completeOnboarding()
            navigateBack()
        },
        onNavigateBack = navigateBack,
        modifier = Modifier.fillMaxSize(),
    )
}
