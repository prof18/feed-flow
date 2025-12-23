package com.prof18.feedflow.android.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.presentation.OnboardingViewModel
import com.prof18.feedflow.shared.ui.onboarding.OnboardingContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val categories by viewModel.suggestedCategoriesState.collectAsState()
    val selectedFeeds by viewModel.selectedFeedsState.collectAsState()
    val expandedCategories by viewModel.expandedCategoriesState.collectAsState()
    val isLoading by viewModel.isLoadingState.collectAsState()

    OnboardingContent(
        categories = categories,
        selectedFeeds = selectedFeeds,
        expandedCategories = expandedCategories,
        isLoading = isLoading,
        onFeedToggle = viewModel::toggleFeedSelection,
        onCategoryToggle = viewModel::toggleCategoryExpansion,
        onComplete = {
            viewModel.completeOnboarding()
            onOnboardingComplete()
        },
        onSkip = {
            viewModel.skipOnboarding()
            onOnboardingComplete()
        },
        modifier = Modifier.fillMaxSize(),
    )
}
