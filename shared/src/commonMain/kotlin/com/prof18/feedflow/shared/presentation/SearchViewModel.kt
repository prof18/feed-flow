package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.shared.domain.DateFormatter
import com.prof18.feedflow.shared.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.shared.domain.mappers.toFeedItem
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val feedRetrieverRepository: FeedRetrieverRepository,
    private val dateFormatter: DateFormatter,
) : BaseViewModel() {

    private val searchMutableState: MutableStateFlow<SearchState> = MutableStateFlow(SearchState.EmptyState)

    @NativeCoroutinesState
    val searchState: StateFlow<SearchState> = searchMutableState.asStateFlow()

    private val searchQueryMutableState = MutableStateFlow("")
    val searchQueryState = searchQueryMutableState.asStateFlow()

    fun clearSearch() {
        searchMutableState.update { SearchState.EmptyState }
    }

    init {
        searchQueryMutableState
            .debounce(500.milliseconds)
            .distinctUntilChanged()
            .onEach {
                if (it.isNotBlank()) {
                    search(it)
                } else {
                    clearSearch()
                }
            }.launchIn(scope)
    }

    fun updateSearchQuery(query: String) {
        searchQueryMutableState.update { query }
    }

    private fun search(query: String) {
        feedRetrieverRepository
            .search(query)
            .onEach { foundFeed ->
                searchMutableState.update {
                    if (foundFeed.isEmpty()) {
                        SearchState.NoDataFound(
                            searchQuery = query,
                        )
                    } else {
                        SearchState.DataFound(
                            foundFeed.map { feedItem ->
                                feedItem.toFeedItem(dateFormatter)
                            }.toImmutableList(),
                        )
                    }
                }
            }
            .launchIn(scope)
    }

    fun onBookmarkClick(feedItemId: FeedItemId, bookmarked: Boolean) {
        scope.launch {
            feedRetrieverRepository.updateBookmarkStatus(feedItemId, bookmarked)
        }
    }

    fun onReadStatusClick(feedItemId: FeedItemId, read: Boolean) {
        scope.launch {
            feedRetrieverRepository.updateReadStatus(feedItemId, read)
        }
    }
}
