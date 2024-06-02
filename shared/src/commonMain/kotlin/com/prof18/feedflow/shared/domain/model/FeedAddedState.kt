package com.prof18.feedflow.shared.domain.model

sealed class FeedAddedState {
    data object FeedNotAdded : FeedAddedState()
    data object Loading : FeedAddedState()
    data class FeedAdded(
        val feedName: String,
    ) : FeedAddedState()

    sealed class Error : FeedAddedState() {
        data object InvalidUrl : Error()
        data object InvalidTitleLink : Error()
    }
}
