package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItemUrlInfo

sealed class DeeplinkFeedState {
    data object Loading : DeeplinkFeedState()
    data class Success(val data: FeedItemUrlInfo) : DeeplinkFeedState()
    data object Error : DeeplinkFeedState()
}
