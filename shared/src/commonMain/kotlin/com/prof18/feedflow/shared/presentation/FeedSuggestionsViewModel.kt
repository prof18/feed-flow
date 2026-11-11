package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.SuggestedFeed
import com.prof18.feedflow.core.model.SuggestedFeedCategory
import com.prof18.feedflow.shared.domain.feed.FeedFetcherRepository
import com.prof18.feedflow.shared.domain.feed.FeedSourcesRepository
import com.prof18.feedflow.shared.domain.feed.suggestions.suggestedFeeds
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationState
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionOperationStatus
import com.prof18.feedflow.shared.presentation.model.FeedSuggestionUndoOffer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSuggestionsViewModel internal constructor(
    private val feedSourcesRepository: FeedSourcesRepository,
    feedFetcherRepository: FeedFetcherRepository,
    private val backgroundScope: CoroutineScope,
    private val addSuggestedFeed: suspend (SuggestedFeed, String) -> Unit = feedSourcesRepository::addSuggestedFeed,
    private val deleteFeed: suspend (FeedSource) -> Unit = { feedSourcesRepository.deleteFeed(it, backUp = false) },
    private val refreshFeeds: suspend () -> Unit = { feedFetcherRepository.fetchFeeds() },
    private val backUp: suspend () -> Unit = feedSourcesRepository::backupSuggestedFeeds,
) : ViewModel() {

    private val suggestedCategoriesMutableState = MutableStateFlow(suggestedFeeds.toPersistentList())
    val suggestedCategoriesState: StateFlow<ImmutableList<SuggestedFeedCategory>> =
        suggestedCategoriesMutableState.asStateFlow()

    private val selectedCategoryIdMutableState = MutableStateFlow(suggestedFeeds.firstOrNull()?.id)
    val selectedCategoryIdState: StateFlow<String?> = selectedCategoryIdMutableState.asStateFlow()

    private val operationStatesMutableState =
        MutableStateFlow<ImmutableMap<String, FeedSuggestionOperationState>>(persistentMapOf())
    val operationStates: StateFlow<ImmutableMap<String, FeedSuggestionOperationState>> =
        operationStatesMutableState.asStateFlow()

    private val undoOfferMutableState = MutableStateFlow<FeedSuggestionUndoOffer?>(null)
    val undoOfferState: StateFlow<FeedSuggestionUndoOffer?> = undoOfferMutableState.asStateFlow()

    private val isLoadingMutableState = MutableStateFlow(true)
    val isLoadingState: StateFlow<Boolean> = isLoadingMutableState.asStateFlow()

    private var subscribedSources: List<FeedSource> = emptyList()

    // Only the rows this visit is working on; subscribed feeds are always shown as added.
    private val pendingStatuses = mutableMapOf<String, FeedSuggestionOperationStatus>()
    private val addedCategories = mutableMapOf<String, String>()
    private var changedInVisit = false
    private var undoGeneration = 0L
    private var undoExpiry: Job? = null
    private var undoJob: Job? = null

    init {
        viewModelScope.launch {
            feedSourcesRepository.getFeedSources().collect { sources ->
                subscribedSources = sources
                publishRows()
                isLoadingMutableState.value = false
            }
        }
    }

    fun selectCategory(categoryId: String) {
        selectedCategoryIdMutableState.update { categoryId }
    }

    fun addFeed(feed: SuggestedFeed, categoryName: String, undoTimeoutMillis: Long = 10_000) {
        val status = operationStates.value[feed.url]?.status
        if (status == FeedSuggestionOperationStatus.Adding || status == FeedSuggestionOperationStatus.Added) return

        pendingStatuses[feed.url] = FeedSuggestionOperationStatus.Adding
        publishRows()
        viewModelScope.launch {
            try {
                addSuggestedFeed(feed, categoryName)
                pendingStatuses[feed.url] = FeedSuggestionOperationStatus.Added
                addedCategories[feed.url] = categoryName
                changedInVisit = true
                offerUndo(feed, categoryName, undoTimeoutMillis)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e(error) { "Unable to add suggested feed" }
                pendingStatuses[feed.url] = FeedSuggestionOperationStatus.AddFailed
            }
            publishRows()
        }
    }

    fun undoLastAdd(generation: Long) {
        val offer = undoOfferMutableState.value?.takeIf { it.generation == generation } ?: return
        clearUndoOffer()
        val source = subscribedSources.firstOrNull { it.url == offer.feedUrl } ?: return
        pendingStatuses.remove(offer.feedUrl)
        addedCategories.remove(offer.feedUrl)
        publishRows()
        undoJob = backgroundScope.launch { logFailure("undo suggested feed") { deleteFeed(source) } }
    }

    fun endVisit() {
        clearUndoOffer()
        pendingStatuses.clear()
        addedCategories.clear()
        publishRows()
        if (!changedInVisit) return
        changedInVisit = false
        val pendingUndo = undoJob
        backgroundScope.launch {
            pendingUndo?.join()
            logFailure("back up suggested feeds") { backUp() }
            logFailure("load articles of suggested feeds") { refreshFeeds() }
        }
    }

    private suspend fun logFailure(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Logger.e(error) { "Unable to $action" }
        }
    }

    private fun offerUndo(feed: SuggestedFeed, categoryName: String, timeoutMillis: Long) {
        undoExpiry?.cancel()
        val offer = FeedSuggestionUndoOffer(feed.name, feed.url, categoryName, ++undoGeneration)
        undoOfferMutableState.value = offer
        undoExpiry = viewModelScope.launch {
            delay(timeoutMillis)
            if (undoOfferMutableState.value == offer) undoOfferMutableState.value = null
        }
    }

    private fun clearUndoOffer() {
        undoExpiry?.cancel()
        undoExpiry = null
        undoOfferMutableState.value = null
    }

    private fun publishRows() {
        val subscribedUrls = subscribedSources.mapTo(mutableSetOf()) { it.url }
        val urls = subscribedUrls + pendingStatuses.keys
        operationStatesMutableState.value = urls.associateWith { url ->
            val status = if (url in subscribedUrls) {
                FeedSuggestionOperationStatus.Added
            } else {
                pendingStatuses.getValue(url)
            }
            FeedSuggestionOperationState(status, addedCategories[url])
        }.toPersistentMap()
    }
}
