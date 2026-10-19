package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.db.Search
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feed.ArticleNavigationRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.SearchRequest
import com.prof18.feedflow.shared.presentation.model.SearchRequestState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
    private val dateFormatter: DateFormatter,
    feedFontSizeRepository: FeedFontSizeRepository,
    private val feedStateRepository: FeedStateRepository,
    private val articleNavigationRepository: ArticleNavigationRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
    private val searchFeeds: (String, FeedFilter?) -> Flow<List<Search>> = feedActionsRepository::search,
) : ViewModel() {

    private val searchMutableState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.EmptyState)
    val searchState: StateFlow<SearchState> = searchMutableState.asStateFlow()

    private val searchRequestMutableState = MutableStateFlow<SearchRequestState>(SearchRequestState.Idle)
    val searchRequestState = searchRequestMutableState.asStateFlow()

    private val searchQueryMutableState = MutableStateFlow("")
    val searchQueryState = searchQueryMutableState.asStateFlow()

    private var currentSearchContextFeedFilter: FeedFilter = feedStateRepository.getCurrentFeedFilter()
    private val searchFilterMutableState = MutableStateFlow(currentSearchContextFeedFilter.toSearchFilter())
    val searchFilterState: StateFlow<SearchFilter> = searchFilterMutableState.asStateFlow()

    private val searchFeedFilterMutableState = MutableStateFlow(currentSearchContextFeedFilter.toSearchFeedFilter())
    val searchFeedFilterState: StateFlow<FeedFilter?> = searchFeedFilterMutableState.asStateFlow()
    private var searchJob: Job? = null
    private var requestGeneration = 0L
    private var currentSearchRequest: SearchRequest? = null
    private val pendingSearchRequests = MutableStateFlow<SearchRequest?>(null)

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    private val mappingSettings = feedAppearanceSettingsRepository.getFeedItemMappingSettings()
    val feedFontSizeState: StateFlow<FeedFontSizes> = feedFontSizeRepository.feedFontSizeState
    val feedItemDisplaySettings: StateFlow<FeedItemDisplaySettings> = combine(
        feedAppearanceSettingsRepository.hideUnreadDot,
        feedAppearanceSettingsRepository.hideFeedSource,
        feedAppearanceSettingsRepository.descriptionLineLimit,
    ) { hideUnreadDot, hideFeedSource, descriptionLineLimit ->
        FeedItemDisplaySettings(
            isHideUnreadDotEnabled = hideUnreadDot,
            isHideFeedSourceEnabled = hideFeedSource,
            descriptionLineLimit = descriptionLineLimit,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FeedItemDisplaySettings())

    init {
        pendingSearchRequests
            .debounce(500.milliseconds)
            .onEach { request ->
                if (request != null && request == currentSearchRequest && searchJob == null) {
                    search(request)
                }
            }.launchIn(viewModelScope)

        feedStateRepository.currentFeedFilter
            .onEach { feedFilter ->
                if (searchQueryMutableState.value.isBlank()) {
                    refreshSearchContext(feedFilter)
                }
            }.launchIn(viewModelScope)

        viewModelScope.launch {
            feedStateRepository.errorState.collect { error ->
                when (error) {
                    is DatabaseError -> {
                        mutableUIErrorState.emit(
                            UIErrorState.DatabaseError(errorCode = error.errorCode),
                        )
                    }

                    is SyncError -> {
                        mutableUIErrorState.emit(
                            UIErrorState.SyncError(errorCode = error.errorCode),
                        )
                    }

                    is DeleteFeedSourceError -> {
                        mutableUIErrorState.emit(UIErrorState.DeleteFeedSourceError)
                    }
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        if (query == searchQueryMutableState.value) return
        searchQueryMutableState.update { query }
        prepareSearch(query)
    }

    fun retrySearch() {
        if (searchRequestMutableState.value !is SearchRequestState.Error) return
        prepareSearch(searchQueryMutableState.value)?.let(::search)
    }

    fun updateSearchFilter(filter: SearchFilter) {
        if (filter == searchFilterMutableState.value) return
        searchFilterMutableState.update { filter }
        prepareSearch(searchQueryMutableState.value)?.let(::search)
    }

    fun isCurrentSearchResult(itemId: String): Boolean {
        val state = searchRequestMutableState.value as? SearchRequestState.Results ?: return false
        val result = state.result as? SearchState.DataFound ?: return false
        return state.request == currentSearchRequest && result.items.any { it.id == itemId }
    }

    fun resetSearch() {
        refreshSearchContext()
        searchQueryMutableState.update { "" }
        clearSearch()
    }

    fun onBookmarkClick(feedItemId: FeedItemId, bookmarked: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }

    fun onReadStatusClick(feedItemId: FeedItemId, read: Boolean) {
        viewModelScope.launch {
            feedActionsRepository.updateReadStatus(feedItemId, read)
        }
    }

    fun markAllAboveAsRead(targetItemId: String) {
        val request = currentSearchRequest
        val currentState = searchMutableState.value
        viewModelScope.launch {
            if (request != currentSearchRequest) return@launch
            if (currentState is SearchState.DataFound) {
                val items = currentState.items
                val targetIndex = items.indexOfFirst { it.id == targetItemId }

                if (targetIndex != -1) {
                    // Get all items from the beginning up to and including the target item
                    val itemsToMark = items.subList(0, targetIndex + 1)
                        .filter { !it.isRead }
                        .map { FeedItemId(it.id) }

                    if (itemsToMark.isNotEmpty()) {
                        feedActionsRepository.markAsRead(itemsToMark.toHashSet())
                    }
                }
            }
        }
    }

    fun markAllBelowAsRead(targetItemId: String) {
        val request = currentSearchRequest
        val currentState = searchMutableState.value
        viewModelScope.launch {
            if (request != currentSearchRequest) return@launch
            if (currentState is SearchState.DataFound) {
                val items = currentState.items
                val targetIndex = items.indexOfFirst { it.id == targetItemId }

                if (targetIndex != -1) {
                    // Get all items from the target item to the end
                    val itemsToMark = items.subList(targetIndex, items.size)
                        .filter { !it.isRead }
                        .map { FeedItemId(it.id) }

                    if (itemsToMark.isNotEmpty()) {
                        feedActionsRepository.markAsRead(itemsToMark.toHashSet())
                    }
                }
            }
        }
    }

    private fun clearSearch() {
        searchJob?.cancel()
        searchJob = null
        articleNavigationRepository.clearSearchResults()
        currentSearchRequest = null
        pendingSearchRequests.value = null
        searchMutableState.update { SearchState.EmptyState }
        searchRequestMutableState.value = SearchRequestState.Idle
    }

    private fun prepareSearch(query: String): SearchRequest? {
        searchJob?.cancel()
        searchJob = null
        if (query.isBlank()) {
            clearSearch()
            return null
        }
        val request = SearchRequest(
            generation = ++requestGeneration,
            query = query,
            filter = searchFilterMutableState.value,
            feedFilter = searchFilterMutableState.value.toFeedFilter(),
        )
        currentSearchRequest = request
        searchRequestMutableState.value = SearchRequestState.Loading(request)
        pendingSearchRequests.value = request
        return request
    }

    override fun onCleared() {
        articleNavigationRepository.clearSearchResults()
        super.onCleared()
    }

    private fun refreshSearchContext() {
        refreshSearchContext(feedStateRepository.getCurrentFeedFilter())
    }

    private fun refreshSearchContext(feedFilter: FeedFilter) {
        currentSearchContextFeedFilter = feedFilter
        searchFilterMutableState.update { currentSearchContextFeedFilter.toSearchFilter() }
        searchFeedFilterMutableState.update { currentSearchContextFeedFilter.toSearchFeedFilter() }
    }

    private fun search(request: SearchRequest) {
        if (request != currentSearchRequest) return
        searchJob = viewModelScope.launch {
            try {
                searchFeeds(request.query, request.feedFilter).collect { foundFeed ->
                    if (request != currentSearchRequest) return@collect
                    val items = foundFeed.map { feedItem ->
                        feedItem.toFeedItem(
                            dateFormatter = dateFormatter,
                            settings = mappingSettings,
                        )
                    }.toImmutableList()
                    if (items.isEmpty()) {
                        articleNavigationRepository.clearSearchResults()
                    } else {
                        articleNavigationRepository.setSearchResults(items)
                    }
                    val result = if (items.isEmpty()) {
                        SearchState.NoDataFound(searchQuery = request.query)
                    } else {
                        SearchState.DataFound(
                            feedLayout = feedAppearanceSettingsRepository.getFeedLayout(),
                            isGridLayoutEnabled = feedAppearanceSettingsRepository.getGridLayoutEnabled(),
                            items = items,
                        )
                    }
                    searchMutableState.value = result
                    searchRequestMutableState.value = SearchRequestState.Results(request, result)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e(error) { "Article search failed" }
                if (request == currentSearchRequest) {
                    searchRequestMutableState.value = SearchRequestState.Error(request)
                }
            }
        }
    }

    private fun SearchFilter.toFeedFilter(): FeedFilter? {
        return when (this) {
            SearchFilter.All -> null
            SearchFilter.CurrentFeed -> currentSearchContextFeedFilter
            SearchFilter.Read -> FeedFilter.Read
            SearchFilter.Bookmarks -> FeedFilter.Bookmarks
        }
    }

    private fun FeedFilter.toSearchFilter(): SearchFilter {
        return when (this) {
            is FeedFilter.Bookmarks -> SearchFilter.Bookmarks
            is FeedFilter.Read -> SearchFilter.Read
            is FeedFilter.Category,
            is FeedFilter.Source,
            FeedFilter.Uncategorized,
            -> SearchFilter.CurrentFeed
            FeedFilter.Timeline -> SearchFilter.All
        }
    }

    private fun FeedFilter.toSearchFeedFilter(): FeedFilter? {
        return when (this) {
            is FeedFilter.Category,
            is FeedFilter.Source,
            FeedFilter.Uncategorized,
            -> this
            FeedFilter.Timeline,
            FeedFilter.Read,
            FeedFilter.Bookmarks,
            -> null
        }
    }
}
