package com.prof18.feedflow

import co.touchlab.kermit.Logger

sealed interface FeedUpdateStatus {
    val refreshedFeedCount: Int
    val totalFeedCount: Int

    fun isLoading(): Boolean {
        val isLoading =  this is StartedFeedUpdateStatus || this is InProgressFeedUpdateStatus
        Logger.d { ">>>> $this" }
        Logger.d { ">>>> IsLoading? $isLoading" }
        return isLoading
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