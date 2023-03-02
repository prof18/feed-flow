package com.prof18.feedflow.feedlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.FeedManagerRepository
import com.prof18.feedflow.domain.model.FeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedSourceListViewModel(
    private val feedManagerRepository: FeedManagerRepository
): ViewModel() {

    private val feedsMutableState: MutableStateFlow<List<FeedSource>> = MutableStateFlow(listOf())
    val feedsState: StateFlow<List<FeedSource>> = feedsMutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val feeds = feedManagerRepository.getFeeds()
            feedsMutableState.update { feeds }
        }
    }
}