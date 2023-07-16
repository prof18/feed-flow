package com.prof18.feedflow.presentation

import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.model.FeedItem
import com.prof18.feedflow.domain.model.FeedItemId
import com.prof18.feedflow.domain.model.FeedUpdateStatus
import com.prof18.feedflow.presentation.model.FeedErrorState
import com.prof18.feedflow.presentation.model.UIErrorState
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    // Loading
    @NativeCoroutinesState
    val loadingState: StateFlow<FeedUpdateStatus> = feedRetrieverRepository.updateState

    // Feeds
    private val mutableFeedState: MutableStateFlow<List<FeedItem>> = MutableStateFlow(emptyList())

    @NativeCoroutinesState
    val feedState = mutableFeedState.asStateFlow()

    @NativeCoroutinesState
    val countState = mutableFeedState.map { it.count { item -> !item.isRead } }

    // Error
    private val mutableUIErrorState: MutableSharedFlow<UIErrorState?> = MutableSharedFlow()

    @NativeCoroutinesState
    val errorState = mutableUIErrorState.asSharedFlow()

    private var lastUpdateIndex = 0

    init {
        observeFeeds()
        getNewFeeds()
    }

    private fun observeFeeds() {
        scope.launch {
            feedRetrieverRepository.getFeeds()
                .collect { feedItems ->
                    mutableFeedState.update {
                        feedItems
                    }
                }
        }

        scope.launch {
            feedRetrieverRepository.errorState
                .collect { error ->
                    when (error) {
                        is FeedErrorState -> {
                            mutableUIErrorState.emit(
                                UIErrorState(
                                    message = StringDesc.ResourceFormatted(
                                        stringRes = MR.strings.feed_error_message,
                                        error.failingSourceName,
                                    ),
                                ),
                            )
                        }

                        null -> {
                            // Do nothing
                        }
                    }
                }
        }
    }

    fun getNewFeeds() {
        lastUpdateIndex = 0
        scope.launch {
            feedRetrieverRepository.fetchFeeds()
        }
    }

    fun updateReadStatus(lastVisibleIndex: Int) {
        // To avoid issues
        if (loadingState.value.isLoading()) {
            return
        }
        val urlToUpdates = mutableListOf<FeedItemId>()

        val items = feedState.value.toMutableList()
        if (lastVisibleIndex <= lastUpdateIndex) {
            return
        }
        for (index in lastUpdateIndex..lastVisibleIndex) {
            val item = items[index]
            if (!item.isRead) {
                urlToUpdates.add(
                    FeedItemId(
                        id = item.id,
                    ),
                )
            }
        }

        lastUpdateIndex = lastVisibleIndex
        scope.launch {
            feedRetrieverRepository.updateReadStatus(urlToUpdates)
        }
    }

    fun markAllRead() {
        scope.launch {
            feedRetrieverRepository.markAllFeedAsRead()
            feedRetrieverRepository.fetchFeeds()
        }
    }

    fun markAsRead(feedItemId: Int) {
        scope.launch {
            feedRetrieverRepository.updateReadStatus(
                listOf(
                    FeedItemId(feedItemId),
                ),
            )
        }
    }

    fun deleteOldFeedItems() {
        scope.launch {
            feedRetrieverRepository.deleteOldFeeds()
        }
    }
}
