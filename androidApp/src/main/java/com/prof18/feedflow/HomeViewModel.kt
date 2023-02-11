package com.prof18.feedflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val feedRetrieverRepository: FeedRetrieverRepository
) : ViewModel() {

    // Loading
    val loadingState: StateFlow<FeedUpdateStatus> = feedRetrieverRepository.updateState

    // Feeds
    private val mutableFeedState: MutableStateFlow<List<FeedItem>> = MutableStateFlow(emptyList())
    val feedState = mutableFeedState.asStateFlow()

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState?> = MutableSharedFlow()
    val errorState = mutableUIErrorState.asSharedFlow()

    private val updateReadStatusFlow = MutableSharedFlow<List<FeedItemId>>()
    private var lastUpdateIndex = 0

    init {
        observeFeeds()
        observeReadStatusFlow()
        getNewFeeds()
    }

    private fun observeFeeds() {
        viewModelScope.launch {
            feedRetrieverRepository.getFeeds()
                .collect { feedItems ->
                    mutableFeedState.update {
                        feedItems
                    }
                }
        }

        viewModelScope.launch {
            feedRetrieverRepository.errorState
                .collect { error ->
                    when (error) {
                        is FeedErrorState -> {
                            mutableUIErrorState.emit(
                                UIErrorState(
                                    message = "Something is wrong with: ${error.failingSourceName} :("
                                )
                            )
                        }

                        NoFeedSourceError -> {
                            mutableUIErrorState.emit(
                                UIErrorState(
                                    message = "There are no sources. Please add some source"
                                )
                            )
                        }

                        null -> {
                            // Do nothing
                        }
                    }
                }
        }
    }

    private fun observeReadStatusFlow() {
        viewModelScope.launch {
            updateReadStatusFlow.collect { itemUrls ->
                feedRetrieverRepository.updateReadStatus(itemUrls)
            }
        }
    }

    fun getNewFeeds() {
        lastUpdateIndex = 0
        viewModelScope.launch {
            feedRetrieverRepository.fetchFeeds()
        }
    }

    fun updateReadStatus(lastVisibleIndex: Int) {
        val urlToUpdates = mutableListOf<FeedItemId>()
        mutableFeedState.update { feedState ->
            val items = feedState.toMutableList()
            for (index in lastUpdateIndex..lastVisibleIndex) {
                val item = items[index]
                if (!item.isRead) {
                    urlToUpdates.add(
                        FeedItemId(
                            id = item.id,
                        )
                    )
                }
                items[index] = items[index].copy(isRead = true)
            }

            items
        }

        lastUpdateIndex = lastVisibleIndex
        viewModelScope.launch {
            updateReadStatusFlow.emit(urlToUpdates)
        }
    }
}
