package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ContentPrefetchRepositoryIosDesktop(
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val dispatcherProvider: DispatcherProvider,
) : ContentPrefetchRepository {

    private var backgroundJob: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    override suspend fun prefetchContent() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return
        }

        try {
            val immediateItems = databaseHelper.getFirstUnfetchedItemsBatch(
                pageSize = ContentPrefetchRepository.FIRST_PAGE_SIZE,
            )
            logger.d { "Found ${immediateItems.size} items for immediate prefetch" }

            for (item in immediateItems) {
                logger.d { "Prefetching: ${item.feedItemId}" }
                prefetchSingleItem(
                    PrefetchQueueItem(
                        feedItemId = item.feedItemId,
                        url = item.url,
                    ),
                )
            }
            val allUnfetched = databaseHelper.getUnfetchedItems()

            val queueItems = allUnfetched.map { item ->
                PrefetchQueueItem(
                    feedItemId = item.feedItemId,
                    url = item.url,
                )
            }

            databaseHelper.insertPrefetchQueueItems(
                items = queueItems,
                currentTimeMillis = Clock.System.now().toEpochMilliseconds(),
            )
            logger.d { "Queued ${queueItems.size} items for background prefetch" }
            startBackgroundFetching()
        } catch (e: Exception) {
            logger.e(e) { "Error in prefetchContent" }
        }
    }

    override suspend fun cancelFetching() {
        backgroundJob?.cancel()
        databaseHelper.clearPrefetchQueue()
    }

    override fun startBackgroundFetching() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return
        }

        if (backgroundJob?.isActive == true) return

        backgroundJob = coroutineScope.launch(dispatcherProvider.io) {
            try {
                val queuedItems = databaseHelper.getNextPrefetchBatch()

                logger.d { "Processing ${queuedItems.size} queued items" }

                for (item in queuedItems) {
                    prefetchSingleItem(item)
                }

                logger.d { "Background prefetch complete. Processed ${queuedItems.size} items" }
            } catch (e: Exception) {
                logger.e(e) { "Error in background prefetch" }
            }
        }
    }

    private suspend fun prefetchSingleItem(item: PrefetchQueueItem) {
        logger.d { "Prefetching: ${item.feedItemId}" }

        val result = feedItemParserWorker.parse(
            feedItemId = item.feedItemId,
            url = item.url,
        )

        when (result) {
            is ParsingResult.Success -> {
                val content = result.htmlContent
                if (content != null) {
                    feedItemContentFileHandler.saveFeedItemContentToFile(item.feedItemId, content)
                    logger.d { "Prefetched successfully: ${item.feedItemId}" }
                } else {
                    logger.d { "Content null for: ${item.feedItemId}" }
                }
                databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                databaseHelper.removePrefetchQueueItem(item.feedItemId)
            }

            is ParsingResult.Error -> {
                logger.d { "Parse failed for: ${item.feedItemId}, skipping permanently" }
                // Mark as fetched to skip permanently (best-effort approach)
                databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                databaseHelper.removePrefetchQueueItem(item.feedItemId)
            }
        }
    }
}
