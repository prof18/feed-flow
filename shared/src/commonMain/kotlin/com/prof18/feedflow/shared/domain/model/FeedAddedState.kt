package com.prof18.feedflow.shared.domain.model

sealed class FeedAddedState {
    data object FeedNotAdded : FeedAddedState()
    data object Loading : FeedAddedState()
    data class FeedAdded(
        val feedName: String? = null,
    ) : FeedAddedState()

    sealed class Error : FeedAddedState() {
        abstract val canForceAdd: Boolean

        data class InvalidUrl(override val canForceAdd: Boolean) : Error()
        data class InvalidTitleLink(override val canForceAdd: Boolean) : Error()
        data class GenericError(override val canForceAdd: Boolean) : Error()
    }
}
