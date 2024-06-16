package com.prof18.feedflow.shared.domain.feedsync

import com.prof18.feedflow.shared.domain.model.SyncResult

// TODO: try to make a common implementation instead of three separate one
interface FeedSyncWorker {
    /**
     * The job will be enqueued not done immediately
     */
    fun upload()

    /**
     * It's always immediate, without waiting
     */
    suspend fun uploadImmediate()

    // All downloads are immediate

    suspend fun downloadAndSyncAll()

    suspend fun download(): SyncResult

    suspend fun syncFeedSources(): SyncResult

    suspend fun syncFeedItems(): SyncResult
}
