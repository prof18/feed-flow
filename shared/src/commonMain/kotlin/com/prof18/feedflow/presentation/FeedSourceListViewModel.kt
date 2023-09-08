package com.prof18.feedflow.presentation

import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private val feedsMutableState: MutableStateFlow<List<FeedSource>> = MutableStateFlow(listOf())

    @NativeCoroutinesState
    val feedSourcesState: StateFlow<List<FeedSource>> = feedsMutableState.asStateFlow()

    init {
        scope.launch {
            feedManagerRepository.getFeedSources().collect { feeds ->
                feedsMutableState.update { feeds }
            }
        }
    }

    fun deleteFeedSource(feedSource: FeedSource) {
        scope.launch {
            feedManagerRepository.deleteFeed(feedSource)
            feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
        }
    }
}
