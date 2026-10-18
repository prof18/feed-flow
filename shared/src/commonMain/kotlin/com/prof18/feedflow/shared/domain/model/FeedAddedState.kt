package com.prof18.feedflow.shared.domain.model

sealed class FeedAddedState {
    data object FeedNotAdded : FeedAddedState()
    data object Loading : FeedAddedState()
    data class FeedAlreadyExists(val feedName: String) : FeedAddedState()
    data class FeedAdded(
        val feedName: String? = null,
        val shortsFilterFallback: Boolean = false,
    ) : FeedAddedState()

    sealed class Error : FeedAddedState() {
        abstract val canForceAdd: Boolean

        data class FetchFailed(
            val statusCode: Int,
            override val canForceAdd: Boolean = true,
            val verificationUrl: String? = null,
            val retryAfter: String? = null,
            val challengedUrl: String? = null,
        ) : Error()

        data class InvalidUrl(override val canForceAdd: Boolean) : Error()
        data class InvalidTitleLink(override val canForceAdd: Boolean) : Error()
        data class GenericError(override val canForceAdd: Boolean) : Error()
    }
}
