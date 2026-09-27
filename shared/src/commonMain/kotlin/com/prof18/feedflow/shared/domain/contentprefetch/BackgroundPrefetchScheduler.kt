package com.prof18.feedflow.shared.domain.contentprefetch

/**
 * Runs [ContentPrefetcher.prefetchQueuedBatch] outside the sync that queued the items:
 * WorkManager on Android, an in-process job on iOS and Desktop.
 */
internal interface BackgroundPrefetchScheduler {
    fun start()
    fun pause()
    suspend fun cancel()
}
