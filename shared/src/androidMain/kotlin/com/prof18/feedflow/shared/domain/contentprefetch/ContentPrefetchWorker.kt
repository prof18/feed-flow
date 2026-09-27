package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CancellationException

internal class ContentPrefetchWorker(
    private val databaseHelper: DatabaseHelper,
    private val kleadFeedItemParserWorker: FeedItemParserWorker,
    private val logger: Logger,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val queuedItems = databaseHelper.getNextPrefetchBatch()
            for (item in queuedItems) {
                prefetchItem(item)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private suspend fun prefetchItem(item: PrefetchQueueItem) {
        val result = kleadFeedItemParserWorker.parse(
            feedItemId = item.feedItemId,
            url = item.url,
        )
        commitPrefetchResult(item, result)
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
                databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                databaseHelper.removePrefetchQueueItem(item.feedItemId)
            }
        }
    }
}
