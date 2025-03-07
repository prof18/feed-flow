package com.prof18.feedflow.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.shared.domain.feed.FeedActionsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import com.prof18.feedflow.shared.presentation.model.DeeplinkFeedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeeplinkFeedViewModel internal constructor(
    private val widgetRepository: FeedWidgetRepository,
    private val feedActionsRepository: FeedActionsRepository,
) : ViewModel() {

    private val deeplinkFeedMutableState = MutableStateFlow<DeeplinkFeedState>(DeeplinkFeedState.Loading)
    val deeplinkFeedState = deeplinkFeedMutableState.asStateFlow()

    fun getReaderModeUrl(feedItemId: FeedItemId) {
        viewModelScope.launch {
            deeplinkFeedMutableState.value = DeeplinkFeedState.Loading
            val url = widgetRepository.getFeedItemById(feedItemId)
            if (url != null) {
                deeplinkFeedMutableState.value = DeeplinkFeedState.Success(url)
            } else {
                deeplinkFeedMutableState.value = DeeplinkFeedState.Error
            }
        }
    }

    fun markAsRead(feedItemId: FeedItemId) {
        viewModelScope.launch {
            feedActionsRepository.markAsRead(hashSetOf(feedItemId))
        }
    }
}
