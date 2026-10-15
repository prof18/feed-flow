package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedAddState
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.suggestions.suggestedFeeds
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSuggestionsViewModel internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
    private val feedFetcherRepository: FeedFetcherRepository,
    private val addSuggestedFeed: suspend (SuggestedFeed, FeedSourceCategory) -> Unit = { feed, category ->
        feedSourcesRepository.addFeedSourceWithoutFetching(feed.url, feed.name, category, feed.logoUrl)
    },
    private val refreshFeeds: suspend () -> Unit = { feedFetcherRepository.fetchFeeds() },
    private val feedExists: suspend (String) -> Boolean = { url ->
        feedSourcesRepository.getFeedSources().first().any { it.url == url }
    },
) : ViewModel() {

    private val suggestedCategoriesMutableState =
        MutableStateFlow<ImmutableList<SuggestedFeedCategory>>(persistentListOf())
    val suggestedCategoriesState: StateFlow<ImmutableList<SuggestedFeedCategory>> =
        suggestedCategoriesMutableState.asStateFlow()

    private val selectedCategoryIdMutableState = MutableStateFlow<String?>(null)
    val selectedCategoryIdState: StateFlow<String?> = selectedCategoryIdMutableState.asStateFlow()

    private val feedStatesMapMutableState = MutableStateFlow<ImmutableMap<String, FeedAddState>>(persistentMapOf())
    val feedStatesMapState: StateFlow<ImmutableMap<String, FeedAddState>> = feedStatesMapMutableState.asStateFlow()

    private val operationStatesMutableState =
        MutableStateFlow<ImmutableMap<String, FeedSuggestionOperationState>>(persistentMapOf())
    val operationStates = operationStatesMutableState.asStateFlow()
    private var operationGeneration = 0L

    private val isLoadingMutableState = MutableStateFlow(true)
    val isLoadingState: StateFlow<Boolean> = isLoadingMutableState.asStateFlow()

    init {
        loadSuggestedFeeds()
        observeExistingFeeds()
    }

    private fun loadSuggestedFeeds() {
        suggestedCategoriesMutableState.update { suggestedFeeds.toPersistentList() }
        if (suggestedFeeds.isNotEmpty()) {
            selectedCategoryIdMutableState.update { suggestedFeeds.first().id }
        }
    }

    private fun observeExistingFeeds() {
        viewModelScope.launch {
            feedSourcesRepository.getFeedSources().collect { existingFeeds ->
                val addedUrls = existingFeeds.map { it.url }.toSet()

                feedStatesMapMutableState.update { currentStates ->
                    val reconciledStates = currentStates
                        .filterValues { it != FeedAddState.Added }
                        .toMutableMap()

                    addedUrls.forEach { feedUrl ->
                        reconciledStates[feedUrl] = FeedAddState.Added
                    }

                    reconciledStates.toPersistentMap()
                }
                isLoadingMutableState.update { false }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        selectedCategoryIdMutableState.update { categoryId }
    }

    fun addFeed(feed: SuggestedFeed, categoryName: String) {
        val currentState = feedStatesMapMutableState.value[feed.url]
        val currentOperation = operationStatesMutableState.value[feed.url]?.status
        if (currentState == FeedAddState.Added || currentState == FeedAddState.Adding ||
            currentOperation == FeedSuggestionOperationStatus.Adding ||
            currentOperation == FeedSuggestionOperationStatus.Refreshing
        ) {
            return
        }

        val generation = ++operationGeneration
        updateFeedOperation(feed.url, generation, FeedAddState.Adding, FeedSuggestionOperationStatus.Adding)
        viewModelScope.launch {
            var addAttempted = false
            var sourceAdded = false
            try {
                if (feedExists(feed.url)) {
                    updateFeedOperation(feed.url, generation, FeedAddState.Added, FeedSuggestionOperationStatus.Added)
                    return@launch
                }
                val category = FeedSourceCategory(
                    title = categoryName,
                    id = categoryName.hashCode().toString(),
                )

                addAttempted = true
                addSuggestedFeed(feed, category)
                sourceAdded = true
                updateFeedOperation(
                    feed.url,
                    generation,
                    FeedAddState.Added,
                    FeedSuggestionOperationStatus.Refreshing,
                )
                refreshFeeds()
                updateFeedOperation(feed.url, generation, FeedAddState.Added, FeedSuggestionOperationStatus.Added)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e(error) { "Unable to finish suggested feed operation" }
                val saved = sourceAdded || feedStatesMapMutableState.value[feed.url] == FeedAddState.Added ||
                    (addAttempted && verifyFeedSaved(feed.url))
                updateFeedOperation(
                    feed.url,
                    generation,
                    if (saved) FeedAddState.Added else FeedAddState.NotAdded,
                    if (saved) FeedSuggestionOperationStatus.UpdateFailed else FeedSuggestionOperationStatus.AddFailed,
                )
            }
        }
    }

    private suspend fun verifyFeedSaved(url: String): Boolean = try {
        feedExists(url)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Logger.e(error) { "Unable to verify suggested feed operation" }
        false
    }

    private fun updateFeedOperation(
        url: String,
        generation: Long,
        feedState: FeedAddState,
        status: FeedSuggestionOperationStatus,
    ) {
        feedStatesMapMutableState.update { (it + (url to feedState)).toPersistentMap() }
        operationStatesMutableState.update {
            (it + (url to FeedSuggestionOperationState(generation, status))).toPersistentMap()
        }
    }
}
