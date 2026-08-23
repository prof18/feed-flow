package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.parser.ParserSelectionCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ContentPrefetchRepositoryIosDesktop(
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
    private val feedItemParserWorker: FeedItemParserWorker,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val dispatcherProvider: DispatcherProvider,
    private val parserSelectionCoordinator: ParserSelectionCoordinator,
) : ContentPrefetchRepository {

    private var backgroundJob: Job? = null
    private var immediateJob: Job? = null
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    override suspend fun prefetchContent() = coroutineScope {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return@coroutineScope
        }

        immediateJob?.cancelAndJoin()
        val job = launch {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Error in prefetchContent" }
            }
        }
        immediateJob = job
        try {
            job.join()
        } finally {
            if (immediateJob === job) immediateJob = null
        }
    }

    override suspend fun cancelFetching() {
        immediateJob?.cancelAndJoin()
        backgroundJob?.cancelAndJoin()
        databaseHelper.clearPrefetchQueue()
    }

    override fun pauseFetching() {
        backgroundJob?.cancel()
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Error in background prefetch" }
            }
        }
    }

    private suspend fun prefetchSingleItem(item: PrefetchQueueItem) {
        logger.d { "Prefetching: ${item.feedItemId}" }
        val selectionSnapshot = parserSelectionCoordinator.snapshot()

        val result = feedItemParserWorker.parse(
            feedItemId = item.feedItemId,
            url = item.url,
        )
        val committed = parserSelectionCoordinator.withSelection(selectionSnapshot) {
            commitPrefetchResult(item, result)
            true
        }
        if (committed == null) {
            logger.d { "Parser changed while prefetching ${item.feedItemId}; discarding result" }
        }
    }

    private suspend fun commitPrefetchResult(item: PrefetchQueueItem, result: ParsingResult) {
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
