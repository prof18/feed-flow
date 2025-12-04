package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import android.webkit.WebView
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.parser.FeedItemParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext

internal class ContentPrefetchWorker(
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val queuedItems = databaseHelper.getNextPrefetchBatch()
            val webView = withContext(dispatcherProvider.main) {
                WebView(appContext)
            }
            for (item in queuedItems) {
                prefetchItem(item, webView)
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    private suspend fun prefetchItem(item: PrefetchQueueItem, webView: WebView) {
        val deferredResult = CompletableDeferred<ParsingResult>()
        FeedItemParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
            webView = webView,
        ).parseFeedItem(item.url) { result ->
            deferredResult.complete(result)
        }
        val result = deferredResult.await()
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
