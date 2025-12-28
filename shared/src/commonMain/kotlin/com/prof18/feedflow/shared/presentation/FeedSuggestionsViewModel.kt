package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.suggestions.getSuggestedFeeds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSuggestionsViewModel internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
) : ViewModel() {

    private val suggestedCategoriesMutableState = MutableStateFlow<List<SuggestedFeedCategory>>(emptyList())
    val suggestedCategoriesState: StateFlow<List<SuggestedFeedCategory>> = suggestedCategoriesMutableState.asStateFlow()

    private val selectedCategoryIdMutableState = MutableStateFlow<String?>(null)
    val selectedCategoryIdState: StateFlow<String?> = selectedCategoryIdMutableState.asStateFlow()

    private val feedStatesMapMutableState = MutableStateFlow<Map<String, FeedAddState>>(emptyMap())
    val feedStatesMapState: StateFlow<Map<String, FeedAddState>> = feedStatesMapMutableState.asStateFlow()

    private val isLoadingMutableState = MutableStateFlow(true)
    val isLoadingState: StateFlow<Boolean> = isLoadingMutableState.asStateFlow()

    init {
        loadSuggestedFeeds()
        loadExistingFeeds()
    }

    private fun loadSuggestedFeeds() {
        val categories = getSuggestedFeeds()
        suggestedCategoriesMutableState.update { categories }
        if (categories.isNotEmpty()) {
            selectedCategoryIdMutableState.update { categories.first().id }
        }
    }

    private fun loadExistingFeeds() {
        viewModelScope.launch {
            val existingFeeds = feedSourcesRepository.getFeedSources().firstOrNull() ?: emptyList()
            val stateMap = existingFeeds.associate { it.url to FeedAddState.Added }
            feedStatesMapMutableState.update { stateMap }
            isLoadingMutableState.update { false }
        }
    }

    fun selectCategory(categoryId: String) {
        selectedCategoryIdMutableState.update { categoryId }
    }

    fun addFeed(feed: SuggestedFeed, categoryName: String) {
        viewModelScope.launch {
            feedStatesMapMutableState.update { it + (feed.url to FeedAddState.Adding) }

            try {
                val category = FeedSourceCategory(
                    title = categoryName,
                    id = categoryName.hashCode().toString(),
                )

                feedSourcesRepository.addFeedSourceWithoutFetching(
                    feedUrl = feed.url,
                    feedTitle = feed.name,
                    category = category,
                    logoUrl = feed.logoUrl,
                )
                feedStatesMapMutableState.update { it + (feed.url to FeedAddState.Added) }
                feedFetcherRepository.fetchFeeds()
            } catch (_: Exception) {
                feedStatesMapMutableState.update { it + (feed.url to FeedAddState.NotAdded) }
            }
        }
    }
}
