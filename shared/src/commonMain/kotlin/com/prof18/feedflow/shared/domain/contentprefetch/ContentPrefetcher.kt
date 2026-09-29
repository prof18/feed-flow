package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feeditem.ReaderContentFetcher

internal class ContentPrefetcher(
    private val logger: Logger,
    private val databaseHelper: DatabaseHelper,
    private val readerContentFetcher: ReaderContentFetcher,
) {
    suspend fun prefetchQueuedBatch() {
        val queuedItems = databaseHelper.getNextPrefetchBatch()
        logger.d { "Processing ${queuedItems.size} queued items" }
        for (item in queuedItems) {
            prefetch(item)
        }
    }

    suspend fun prefetch(item: PrefetchQueueItem) {
        logger.d { "Prefetching: ${item.feedItemId}" }
        val content = readerContentFetcher.fetch(item.feedItemId, item.url)
        if (content != null) {
            logger.d { "Prefetched successfully: ${item.feedItemId}" }
        } else {
            logger.d { "Parse failed for: ${item.feedItemId}, skipping permanently" }
        }
        // Failed items are marked as fetched too: prefetch is best-effort.
        databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
        databaseHelper.removePrefetchQueueItem(item.feedItemId)
    }
}
