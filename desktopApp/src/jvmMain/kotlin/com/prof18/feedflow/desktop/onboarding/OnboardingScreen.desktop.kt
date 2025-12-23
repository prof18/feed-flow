package com.prof18.feedflow.desktop.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.presentation.OnboardingViewModel
import com.prof18.feedflow.shared.ui.onboarding.OnboardingContent
import kotlinx.collections.immutable.toPersistentList

internal class OnboardingScreen : Screen {

    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<OnboardingViewModel>() }
        val suggestedCategories by viewModel.suggestedCategoriesState.collectAsState()
        val selectedFeeds by viewModel.selectedFeedsState.collectAsState()
        val isLoading by viewModel.isLoadingState.collectAsState()
        val expandedCategories by viewModel.expandedCategoriesState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow

        OnboardingContent(
            suggestedCategories = suggestedCategories.toPersistentList(),
            selectedFeeds = selectedFeeds.toPersistentList(),
            expandedCategories = expandedCategories.toPersistentList(),
            isLoading = isLoading,
            onFeedToggle = { feedUrl ->
                viewModel.toggleFeedSelection(feedUrl)
            },
            onCategoryToggle = { categoryId ->
                viewModel.toggleCategoryExpansion(categoryId)
            },
            onContinue = {
                viewModel.completeOnboarding()
                navigator.pop()
            },
            onSkip = {
                viewModel.skipOnboarding()
                navigator.pop()
            },
        )
    }
}
