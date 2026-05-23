package com.prof18.feedflow.shared.domain.feed

class PendingReadStatusActionRetrier internal constructor(
    private val feedActionsRepository: FeedActionsRepository,
) {
    suspend fun retryPendingReadStatusActions() {
        feedActionsRepository.retryPendingReadStatusActions()
    }
}
