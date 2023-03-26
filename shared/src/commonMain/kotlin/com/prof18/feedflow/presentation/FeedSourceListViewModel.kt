package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.feedmanager.FeedManagerRepository
import com.prof18.feedflow.domain.model.FeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel(
    private val feedManagerRepository: FeedManagerRepository
): BaseViewModel() {

    private val feedsMutableState: MutableStateFlow<List<FeedSource>> = MutableStateFlow(listOf())
    val feedsState: StateFlow<List<FeedSource>> = feedsMutableState.asStateFlow()

    init {
        scope.launch {
            val feeds = feedManagerRepository.getFeeds()
            feedsMutableState.update { feeds }
        }
    }
}