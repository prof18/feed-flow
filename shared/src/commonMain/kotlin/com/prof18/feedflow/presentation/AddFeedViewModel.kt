package com.prof18.feedflow.presentation

import com.prof18.feedflow.domain.feed.manager.FeedManagerRepository
import com.prof18.feedflow.domain.feed.retriever.FeedRetrieverRepository
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddFeedViewModel internal constructor(
    private val feedManagerRepository: FeedManagerRepository,
    private val feedRetrieverRepository: FeedRetrieverRepository,
) : BaseViewModel() {

    private var feedName: String = ""
    private var feedUrl: String = ""

    private val isAddDoneMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val isInvalidRssFeedMutableState: MutableStateFlow<Boolean> = MutableStateFlow(false)

    @NativeCoroutinesState
    val isAddDoneState = isAddDoneMutableState.asStateFlow()

    @NativeCoroutinesState
    val isInvalidRssFeed = isInvalidRssFeedMutableState.asStateFlow()

    fun updateFeedUrlTextFieldValue(feedUrlTextFieldValue: String) {
        feedUrl = feedUrlTextFieldValue
        isInvalidRssFeedMutableState.update { false }
    }

    fun updateFeedNameTextFieldValue(feedNameTextFieldValue: String) {
        feedName = feedNameTextFieldValue
    }

    fun addFeed() {
        scope.launch {
            if (feedUrl.isNotEmpty() && feedName.isNotEmpty()) {
                val isValidRss = feedManagerRepository.checkIfValidRss(feedUrl)
                if (isValidRss) {
                    feedManagerRepository.addFeed(
                        url = feedUrl,
                        name = feedName,
                    )
                    isAddDoneMutableState.update { true }
                    feedRetrieverRepository.fetchFeeds(updateLoadingInfo = false)
                } else {
                    isInvalidRssFeedMutableState.update { true }
                }
            }
        }
    }

    fun clearAddDoneState() {
        isAddDoneMutableState.update { false }
    }
}
