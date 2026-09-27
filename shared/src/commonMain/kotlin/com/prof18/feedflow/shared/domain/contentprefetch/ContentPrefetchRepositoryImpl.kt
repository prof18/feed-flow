package com.prof18.feedflow.shared.domain.contentprefetch

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ContentPrefetchRepositoryImpl(
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
    private val contentPrefetcher: ContentPrefetcher,
    private val backgroundPrefetchScheduler: BackgroundPrefetchScheduler,
) : ContentPrefetchRepository {

    private var immediateJob: Job? = null

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
                    contentPrefetcher.prefetch(PrefetchQueueItem(feedItemId = item.feedItemId, url = item.url))
                }

                val queueItems = databaseHelper.getUnfetchedItems().map { item ->
                    PrefetchQueueItem(feedItemId = item.feedItemId, url = item.url)
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

    override fun startBackgroundFetching() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return
        }
        backgroundPrefetchScheduler.start()
    }

    override suspend fun cancelFetching() {
        immediateJob?.cancelAndJoin()
        backgroundPrefetchScheduler.cancel()
        databaseHelper.clearPrefetchQueue()
    }

    override fun pauseFetching() {
        backgroundPrefetchScheduler.pause()
    }
}
