package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.core.model.SyncResult

internal interface FeedSyncWorker {
    /**
     * The job will be enqueued not done immediately
     */
    fun upload()

    /**
     * It's always immediate, without waiting
     */
    suspend fun uploadImmediate()

    suspend fun download(isFirstSync: Boolean = false): SyncResult

    suspend fun syncFeedSources(): SyncResult

    suspend fun syncFeedItems(): SyncResult
}
