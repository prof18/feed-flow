package com.prof18.feedflow

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.*
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
    private val mutableErrorState: MutableStateFlow<ErrorState?> = MutableStateFlow(null)
    val errorState = mutableErrorState.asStateFlow()

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
                    Logger.d { "Got feeds on VM" }
                    mutableFeedState.update {
                        feedItems
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
            try {
                feedRetrieverRepository.fetchFeeds()
            } catch (e: NoFeedException) {
                mutableErrorState.update {
                    ErrorState(
                        message = "There are no sources. Please add some source"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mutableErrorState.update {
                    ErrorState(
                        message = "Something is wrong :("
                    )
                }
            }
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

    fun clearErrorState() = mutableErrorState.update { null }
}
