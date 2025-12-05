package com.prof18.feedflow.shared.domain.contentprefetch

import android.content.Context
import android.webkit.WebView
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.FeedItemToPrefetch
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.model.PrefetchQueueItem
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.database.DatabaseHelper
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.contentprefetch.ContentPrefetchRepository.Companion.FIRST_PAGE_SIZE
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.parser.FeedItemParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ContentPrefetchRepositoryAndroid(
    private val logger: Logger,
    private val settingsRepository: SettingsRepository,
    private val databaseHelper: DatabaseHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : ContentPrefetchRepository {
    override suspend fun prefetchContent() {
        if (!settingsRepository.isPrefetchArticleContentEnabled()) {
            logger.d { "Content prefetch is disabled" }
            return
        }

        try {
            val immediateItems = databaseHelper.getUnfetchedItems(pageSize = FIRST_PAGE_SIZE, offset = 0L)
            logger.d { "Found ${immediateItems.size} items for immediate prefetch" }

            val webView = withContext(dispatcherProvider.main) {
                WebView(appContext)
            }

            for (item in immediateItems) {
                prefetchItem(item, webView)
            }
            val allUnfetched = databaseHelper.getUnfetchedItems(pageSize = -1, offset = 0L)

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
            logger.e(e) { "Error in onFeedSyncCompleted" }
        }
    }

    private suspend fun prefetchItem(
        item: FeedItemToPrefetch,
        webView: WebView,
    ) {
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

    override fun startBackgroundFetching() {
        logger.d { "Enqueueing background content prefetch" }

        val workRequest = OneTimeWorkRequestBuilder<ContentPrefetchWorker>()
            .addTag(WORKER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(
                WORKER_TAG,
                ExistingWorkPolicy.REPLACE,
                workRequest,
            )
    }

    override suspend fun cancelFetching() {
        databaseHelper.clearPrefetchQueue()
        WorkManager.getInstance(appContext).cancelUniqueWork(WORKER_TAG)
    }

    private companion object {
        const val WORKER_TAG = "ContentPrefetchWorker"
    }
}
