package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.parser.ParserSelectionCoordinator
import kotlinx.coroutines.CancellationException

internal class ContentPrefetchWorker(
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val kleadFeedItemParserWorker: FeedItemParserWorker,
    private val logger: Logger,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val parserSelectionCoordinator: ParserSelectionCoordinator,
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val legacyParser = LegacyContentPrefetchParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        )
        return try {
            val queuedItems = databaseHelper.getNextPrefetchBatch()
            for (item in queuedItems) {
                prefetchItem(item, legacyParser)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.failure()
        } finally {
            legacyParser.close()
        }
    }

    private suspend fun prefetchItem(
        item: PrefetchQueueItem,
        legacyParser: LegacyContentPrefetchParser,
    ) {
        val selectionSnapshot = parserSelectionCoordinator.snapshot()
        val result = if (selectionSnapshot.useKleadParser) {
            kleadFeedItemParserWorker.parse(
                feedItemId = item.feedItemId,
                url = item.url,
            )
        } else {
            legacyParser.parse(item.url)
        }
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
                databaseHelper.updateContentFetchedStatus(item.feedItemId, fetched = true)
                databaseHelper.removePrefetchQueueItem(item.feedItemId)
            }
        }
    }
}
