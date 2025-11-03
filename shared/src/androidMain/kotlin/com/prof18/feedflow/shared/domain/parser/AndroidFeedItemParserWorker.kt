package com.prof18.feedflow.shared.domain.parser

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.CompletableDeferred
import java.security.MessageDigest

internal class AndroidFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val appContext: Context,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : FeedItemParserWorker {

    override suspend fun enqueueParsing(url: String) {
        logger.d { "Enqueue parsing not implemented yet (Phase 5): $url" }
    }

    override suspend fun triggerImmediateParsing(url: String): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url" }

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
                val feedItemId = url.toFeedItemId()
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached content for: $url" }
            }
        }

        return result
    }

    override suspend fun triggerBackgroundParsing(url: String): ParsingResult {
        logger.d { "Background parsing not implemented yet (Phase 5): $url" }
        return ParsingResult.Error
    }

    private fun String.toFeedItemId(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(this.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
