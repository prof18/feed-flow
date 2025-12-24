package com.prof18.feedflow.desktop.feedsuggestions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.shared.presentation.FeedSuggestionsViewModel
import com.prof18.feedflow.shared.ui.onboarding.FeedSuggestionsContent
import kotlinx.collections.immutable.toPersistentList

internal class FeedSuggestionsScreen : Screen {

    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<FeedSuggestionsViewModel>() }
        val suggestedCategories by viewModel.suggestedCategoriesState.collectAsState()
        val selectedFeeds by viewModel.selectedFeedsState.collectAsState()
        val isLoading by viewModel.isLoadingState.collectAsState()
        val expandedCategories by viewModel.expandedCategoriesState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow

        FeedSuggestionsContent(
            categories = suggestedCategories.toPersistentList(),
            selectedFeeds = selectedFeeds.toSet(),
            expandedCategories = expandedCategories.toSet(),
            isLoading = isLoading,
            onFeedToggle = { feedUrl ->
                viewModel.toggleFeedSelection(feedUrl)
            },
            onCategoryToggle = { categoryId ->
                viewModel.toggleCategoryExpansion(categoryId)
            },
            onAddFeeds = {
                viewModel.completeOnboarding()
                // TODO: navigate only when the adding process is done
                navigator.pop()
            },
            onNavigateBack = {
                navigator.pop()
            },
        )
    }
}
