package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.data.FeedAppearanceSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedFontSizeRepository
import com.prof18.feedflow.shared.domain.feed.FeedStateRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.prof18.feedflow.shared.presentation.model.DatabaseError
import com.prof18.feedflow.shared.presentation.model.DeleteFeedSourceError
import com.prof18.feedflow.shared.presentation.model.FeedErrorState
import com.prof18.feedflow.shared.presentation.model.SyncError
import com.prof18.feedflow.shared.presentation.model.UIErrorState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SearchViewModel internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
    private val dateFormatter: DateFormatter,
    private val feedFontSizeRepository: FeedFontSizeRepository,
    private val feedStateRepository: FeedStateRepository,
    private val feedAppearanceSettingsRepository: FeedAppearanceSettingsRepository,
) : ViewModel() {

    private val searchMutableState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.EmptyState)
    val searchState: StateFlow<SearchState> = searchMutableState.asStateFlow()

    private val searchQueryMutableState = MutableStateFlow("")
    val searchQueryState = searchQueryMutableState.asStateFlow()

    private var currentSearchContextFeedFilter: FeedFilter = feedStateRepository.getCurrentFeedFilter()
    private val searchFilterMutableState = MutableStateFlow(currentSearchContextFeedFilter.toSearchFilter())
    val searchFilterState: StateFlow<SearchFilter> = searchFilterMutableState.asStateFlow()

    private val searchFeedFilterMutableState = MutableStateFlow(currentSearchContextFeedFilter.toSearchFeedFilter())
    val searchFeedFilterState: StateFlow<FeedFilter?> = searchFeedFilterMutableState.asStateFlow()
    private var searchJob: Job? = null

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    private val isRemoveTitleFromDescriptionEnabled: Boolean =
        feedAppearanceSettingsRepository.getRemoveTitleFromDescription()
    private val hideDate: Boolean = feedAppearanceSettingsRepository.getHideDate()
    private val dateFormat: DateFormat = feedAppearanceSettingsRepository.getDateFormat()
    private val timeFormat: TimeFormat = feedAppearanceSettingsRepository.getTimeFormat()
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

    private val feedLayout = feedAppearanceSettingsRepository.getFeedLayout()

    init {
        searchQueryMutableState
            .debounce(500.milliseconds)
            .distinctUntilChanged()
            .onEach {
                if (it.isNotBlank()) {
                    Logger.d { "Searching for $it" }
                    search(it)
                } else {
                    clearSearch()
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
                    is FeedErrorState -> {
                        mutableUIErrorState.emit(
                            UIErrorState.FeedErrorState(
                                feedName = error.failingSourceName,
                            ),
                        )
                    }

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
        searchQueryMutableState.update { query }
    }

    fun updateSearchFilter(filter: SearchFilter) {
        searchFilterMutableState.update { filter }
        val currentQuery = searchQueryMutableState.value
        if (currentQuery.isNotBlank()) {
            search(currentQuery)
        }
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
        viewModelScope.launch {
            val currentState = searchMutableState.value
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
        viewModelScope.launch {
            val currentState = searchMutableState.value
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
        searchMutableState.update { SearchState.EmptyState }
    }

    private fun refreshSearchContext() {
        refreshSearchContext(feedStateRepository.getCurrentFeedFilter())
    }

    private fun refreshSearchContext(feedFilter: FeedFilter) {
        currentSearchContextFeedFilter = feedFilter
        searchFilterMutableState.update { currentSearchContextFeedFilter.toSearchFilter() }
        searchFeedFilterMutableState.update { currentSearchContextFeedFilter.toSearchFeedFilter() }
    }

    private fun search(query: String) {
        val currentSearchFilter = searchFilterMutableState.value
        val feedFilter = currentSearchFilter.toFeedFilter()
        searchJob?.cancel()
        searchJob = feedActionsRepository
            .search(
                query = query,
                feedFilter = feedFilter,
            )
            .onEach { foundFeed ->
                searchMutableState.update {
                    if (foundFeed.isEmpty()) {
                        SearchState.NoDataFound(
                            searchQuery = query,
                        )
                    } else {
                        SearchState.DataFound(
                            feedLayout = feedLayout,
                            items = foundFeed.map { feedItem ->
                                feedItem.toFeedItem(
                                    dateFormatter = dateFormatter,
                                    removeTitleFromDesc = isRemoveTitleFromDescriptionEnabled,
                                    hideDate = hideDate,
                                    dateFormat = dateFormat,
                                    timeFormat = timeFormat,
                                )
                            }.toImmutableList(),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
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
