package com.prof18.feedflow.domain.model

sealed interface FeedUpdateStatus {
    val refreshedFeedCount: Int
    val totalFeedCount: Int

    fun isLoading(): Boolean {
        return this is StartedFeedUpdateStatus || this is InProgressFeedUpdateStatus
    }
}

object NoFeedSourcesStatus: FeedUpdateStatus {
    override val refreshedFeedCount: Int = 0
    override val totalFeedCount: Int = 0
}

object StartedFeedUpdateStatus : FeedUpdateStatus {
    override val refreshedFeedCount: Int = 0
    override val totalFeedCount: Int = 0
}

data class InProgressFeedUpdateStatus(
    override val refreshedFeedCount: Int,
    override val totalFeedCount: Int,
) : FeedUpdateStatus

data class FinishedFeedUpdateStatus(
    override val refreshedFeedCount: Int,
    override val totalFeedCount: Int,
) : FeedUpdateStatus
