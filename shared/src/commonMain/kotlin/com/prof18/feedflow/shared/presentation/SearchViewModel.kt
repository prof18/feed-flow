package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.data.SettingsRepository
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class SearchViewModel internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
    private val dateFormatter: DateFormatter,
    private val feedFontSizeRepository: FeedFontSizeRepository,
    private val feedStateRepository: FeedStateRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val searchMutableState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.EmptyState)
    val searchState: StateFlow<SearchState> = searchMutableState.asStateFlow()

    private val searchQueryMutableState = MutableStateFlow("")
    val searchQueryState = searchQueryMutableState.asStateFlow()

    private val mutableUIErrorState: MutableSharedFlow<UIErrorState> = MutableSharedFlow()
    val errorState: SharedFlow<UIErrorState> = mutableUIErrorState.asSharedFlow()

    private val isRemoveTitleFromDescriptionEnabled: Boolean = settingsRepository.getRemoveTitleFromDescription()
    private val hideDate: Boolean = settingsRepository.getHideDate()
    private val dateFormat: DateFormat = settingsRepository.getDateFormat()
    private val timeFormat: TimeFormat = settingsRepository.getTimeFormat()

    val feedFontSizeState: StateFlow<FeedFontSizes> = feedFontSizeRepository.feedFontSizeState

    private val feedLayout = settingsRepository.getFeedLayout()

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

    private fun clearSearch() {
        searchMutableState.update { SearchState.EmptyState }
    }

    private fun search(query: String) {
        val currentFeedFilter = feedStateRepository.getCurrentFeedFilter()
        val showReadItems = settingsRepository.getShowReadArticlesTimeline()

        feedActionsRepository
            .search(
                query = query,
                feedFilter = currentFeedFilter,
                showReadItems = showReadItems,
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
}
