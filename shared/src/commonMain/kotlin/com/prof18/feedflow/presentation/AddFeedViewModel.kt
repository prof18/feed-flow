package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private var feedName: String = ""
    private var feedUrl: String = ""

    private val isAddDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isAddDoneState = isAddDoneMutableState.asStateFlow()

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: String) {
        feedName = feedNameTextFieldValue
    }

    // TODO: handle category
    fun addFeed() {
        scope.launch {
            feedManagerRepository.addFeed(
                url = feedUrl,
                name = feedName,
            )
            isAddDoneMutableState.update { true }
            feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
        }
    }
}
