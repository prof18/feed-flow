package com.prof18.feedflow

sealed interface FeedUpdateStatus {
    val refreshedFeedCount: Int
    val totalFeedCount: Int

    fun isLoading(): Boolean =
        this !is FinishedFeedUpdateStatus
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