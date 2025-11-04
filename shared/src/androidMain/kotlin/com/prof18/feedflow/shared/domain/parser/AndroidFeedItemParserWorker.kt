package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CompletableDeferred

internal class AndroidFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : FeedItemParserWorker {

    override suspend fun enqueueParsing(feedItemId: String, url: String) {
        logger.d { "Enqueue parsing not implemented yet (Phase 5): $url" }
    }

    override suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        val deferredResult = CompletableDeferred<ParsingResult>()
        FeedItemParser(
            htmlRetriever = htmlRetriever,
            appContext = appContext,
            logger = logger,
            dispatcherProvider = dispatcherProvider,
        ).parseFeedItem(url) { result ->
            deferredResult.complete(result)
        }
        val result = deferredResult.await()

        if (result is ParsingResult.Success) {
            val content = result.htmlContent
            if (content != null) {
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }
            }
        }

        return result
    }

    override suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Background parsing not implemented yet (Phase 5): $url" }
        return ParsingResult.Error
    }
}
