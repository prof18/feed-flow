package com.prof18.feedflow.shared.domain.model

sealed class FeedEditedState {
    data object Idle : FeedEditedState()
    data object Loading : FeedEditedState()
    data class FeedEdited(
        val feedName: String,
    ) : FeedEditedState()

    sealed class Error : FeedEditedState() {
        data class FetchFailed(val statusCode: Int) : Error()

        data object InvalidUrl : Error()
        data object InvalidTitleLink : Error()
        data object GenericError : Error()
    }
}
