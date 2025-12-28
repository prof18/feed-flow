package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.suggestions.suggestedFeeds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
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

    private val suggestedCategoriesMutableState =
        MutableStateFlow<ImmutableList<SuggestedFeedCategory>>(persistentListOf())
    val suggestedCategoriesState: StateFlow<ImmutableList<SuggestedFeedCategory>> =
        suggestedCategoriesMutableState.asStateFlow()

    private val selectedCategoryIdMutableState = MutableStateFlow<String?>(null)
    val selectedCategoryIdState: StateFlow<String?> = selectedCategoryIdMutableState.asStateFlow()

    private val feedStatesMapMutableState = MutableStateFlow<ImmutableMap<String, FeedAddState>>(persistentMapOf())
    val feedStatesMapState: StateFlow<ImmutableMap<String, FeedAddState>> = feedStatesMapMutableState.asStateFlow()

    private val isLoadingMutableState = MutableStateFlow(true)
    val isLoadingState: StateFlow<Boolean> = isLoadingMutableState.asStateFlow()

    init {
        loadSuggestedFeeds()
        loadExistingFeeds()
    }

    private fun loadSuggestedFeeds() {
        suggestedCategoriesMutableState.update { suggestedFeeds.toPersistentList() }
        if (suggestedFeeds.isNotEmpty()) {
            selectedCategoryIdMutableState.update { suggestedFeeds.first().id }
        }
    }

    private fun loadExistingFeeds() {
        viewModelScope.launch {
            val existingFeeds = feedSourcesRepository.getFeedSources().firstOrNull() ?: emptyList()
            val stateMap = existingFeeds.associate { it.url to FeedAddState.Added }
            feedStatesMapMutableState.update { stateMap.toPersistentMap() }
            isLoadingMutableState.update { false }
        }
    }

    fun selectCategory(categoryId: String) {
        selectedCategoryIdMutableState.update { categoryId }
    }

    fun addFeed(feed: SuggestedFeed, categoryName: String) {
        viewModelScope.launch {
            feedStatesMapMutableState.update { (it + (feed.url to FeedAddState.Adding)).toPersistentMap() }

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
                feedStatesMapMutableState.update { (it + (feed.url to FeedAddState.Added)).toPersistentMap() }
                feedFetcherRepository.fetchFeeds()
            } catch (_: Exception) {
                feedStatesMapMutableState.update { (it + (feed.url to FeedAddState.NotAdded)).toPersistentMap() }
            }
        }
    }
}
