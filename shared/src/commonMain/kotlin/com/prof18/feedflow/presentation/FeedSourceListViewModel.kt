package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.model.FeedSource
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel(
    private val feedManagerRepository: FeedManagerRepository
): BaseViewModel() {

    private val feedsMutableState: MutableStateFlow<List<FeedSource>> = MutableStateFlow(listOf())
    @NativeCoroutinesState
    val feedsState: StateFlow<List<FeedSource>> = feedsMutableState.asStateFlow()

    init {
        scope.launch {
            val feeds = feedManagerRepository.getFeeds()
            feedsMutableState.update { feeds }
        }
    }
}