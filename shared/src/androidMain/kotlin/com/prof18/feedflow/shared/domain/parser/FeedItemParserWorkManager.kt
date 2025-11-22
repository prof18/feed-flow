package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import kotlinx.coroutines.CompletableDeferred

internal class FeedItemParserWorkManager internal constructor(
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val appContext: Context,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val feedItemId = inputData.getString(FEED_ITEM_ID_INPUT_KEY) ?: return Result.failure()
        val url = inputData.getString(URL_INPUT_KEY) ?: return Result.failure()

        logger.d { "Background parsing for feedItemId: $feedItemId, url: $url" }

        val deferredResult = CompletableDeferred<ParsingResult>()
        FeedItemParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        ).parseFeedItem(url) { result ->
            deferredResult.complete(result)
        }

        return when (val result = deferredResult.await()) {
            is ParsingResult.Success -> {
                try {
                    result.htmlContent?.let {
                        // TODO: Save content only if the settings is set to do so
                        feedItemContentFileHandler.saveFeedItemContentToFile(
                            feedItemId = feedItemId,
                            content = it,
                        )
                    }
                    logger.d { "Background parsing succeeded for feedItemId: $feedItemId" }
                    Result.success()
                } catch (e: Exception) {
                    logger.e(e) { "Error saving content to file for feedItemId: $feedItemId" }
                    Result.failure()
                }
            }

            is ParsingResult.Error -> {
                logger.e { "Background parsing failed for feedItemId: $feedItemId" }
                Result.failure()
            }
        }
    }

    companion object {
        const val FEED_ITEM_ID_INPUT_KEY = "FEED_ITEM_ID_INPUT_KEY"
        const val URL_INPUT_KEY = "URL_INPUT_KEY"
    }
}
