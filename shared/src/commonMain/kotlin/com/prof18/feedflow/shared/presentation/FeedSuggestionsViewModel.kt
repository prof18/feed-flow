package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.SuggestedFeedsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSuggestionsViewModel internal constructor(
    private val suggestedFeedsRepository: SuggestedFeedsRepository,
    private val feedSourcesRepository: FeedSourcesRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val suggestedCategoriesMutableState = MutableStateFlow<List<SuggestedFeedCategory>>(emptyList())
    val suggestedCategoriesState: StateFlow<List<SuggestedFeedCategory>> = suggestedCategoriesMutableState.asStateFlow()

    private val selectedFeedsMutableState = MutableStateFlow<Set<String>>(emptySet())
    val selectedFeedsState: StateFlow<Set<String>> = selectedFeedsMutableState.asStateFlow()

    private val isLoadingMutableState = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = isLoadingMutableState.asStateFlow()

    private val expandedCategoriesMutableState = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategoriesState: StateFlow<Set<String>> = expandedCategoriesMutableState.asStateFlow()

    init {
        loadSuggestedFeeds()
    }

    private fun loadSuggestedFeeds() {
        val categories = suggestedFeedsRepository.getSuggestedFeeds()
        suggestedCategoriesMutableState.update { categories }
        if (categories.isNotEmpty()) {
            expandedCategoriesMutableState.update { setOf(categories.first().id) }
        }
    }

    fun toggleFeedSelection(feedUrl: String) {
        selectedFeedsMutableState.update { currentSelection ->
            if (currentSelection.contains(feedUrl)) {
                currentSelection - feedUrl
            } else {
                currentSelection + feedUrl
            }
        }
    }

    fun toggleCategoryExpansion(categoryId: String) {
        expandedCategoriesMutableState.update { currentExpanded ->
            if (currentExpanded.contains(categoryId)) {
                currentExpanded - categoryId
            } else {
                currentExpanded + categoryId
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            isLoadingMutableState.update { true }

            selectedFeedsMutableState.value.forEach { feedUrl ->
                feedSourcesRepository.addFeedSource(
                    feedUrl = feedUrl,
                    categoryName = null,
                    isNotificationEnabled = false,
                )
            }

            isLoadingMutableState.update { false }
        }
    }
}
