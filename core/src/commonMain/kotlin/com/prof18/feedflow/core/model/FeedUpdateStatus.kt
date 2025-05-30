package com.prof18.feedflow.core.model

sealed interface FeedUpdateStatus {
    val refreshedFeedCount: Int
    val totalFeedCount: Int

    fun isLoading(): Boolean {
        return this is StartedFeedUpdateStatus || this is InProgressFeedUpdateStatus
    }
}

data object NoFeedSourcesStatus : FeedUpdateStatus {
    override val refreshedFeedCount: Int = 0
    override val totalFeedCount: Int = 0
}

data object StartedFeedUpdateStatus : FeedUpdateStatus {
    override val refreshedFeedCount: Int = 0
    override val totalFeedCount: Int = 0
}

data class InProgressFeedUpdateStatus(
    override val refreshedFeedCount: Int,
    override val totalFeedCount: Int,
) : FeedUpdateStatus

data object FinishedFeedUpdateStatus : FeedUpdateStatus {
    override val refreshedFeedCount: Int = 0
    override val totalFeedCount: Int = 0
}
