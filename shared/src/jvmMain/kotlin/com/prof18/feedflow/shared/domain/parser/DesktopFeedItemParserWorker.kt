package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import kotlinx.coroutines.withContext
import net.dankito.readability4j.extended.Readability4JExtended

internal class DesktopFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
) : FeedItemParserWorker {

    override suspend fun enqueueParsing(feedItemId: String, url: String) {
        logger.d { "Enqueueing parsing for feedItemId: $feedItemId, url: $url" }
        // On Desktop, we don't have WorkManager, so just trigger parsing directly on background thread
        // This is fire-and-forget like Android WorkManager
        withContext(dispatcherProvider.io) {
            try {
                triggerBackgroundParsing(feedItemId, url)
            } catch (e: Exception) {
                logger.e(e) { "Error enqueueing parsing for: $url" }
            }
        }
    }

    override suspend fun triggerImmediateParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering immediate parsing for: $url (feedItemId: $feedItemId)" }

        return withContext(dispatcherProvider.io) {
            try {
                val html = htmlRetriever.retrieveHtml(url)
                if (html == null) {
                    logger.e { "Failed to fetch HTML for: $url" }
                    return@withContext ParsingResult.Error
                }

                val readability4J = Readability4JExtended(url, html)
                val article = try {
                    readability4J.parse()
                } catch (e: Throwable) {
                    logger.e(e) { "Failed to parse article with Readability4J: $url" }
                    null
                }

                if (article == null) {
                    logger.e { "Readability4J returned null for: $url" }
                    return@withContext ParsingResult.Error
                }

                val content = article.contentWithDocumentsCharsetOrUtf8
                    ?.replace(Regex("https?://.*?placeholder\\.png"), "")

                if (content == null) {
                    logger.e { "No content extracted for: $url" }
                    return@withContext ParsingResult.Error
                }

                val plainText = article.textContent ?: ""
                if (plainText.length < 200) {
                    logger.w { "Content too short (${plainText.length} chars), rejecting: $url" }
                    return@withContext ParsingResult.Error
                }

                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }

                ParsingResult.Success(
                    htmlContent = content,
                    title = article.title,
                    siteName = null,
                )
            } catch (e: Exception) {
                logger.e(e) { "Error parsing content for: $url" }
                ParsingResult.Error
            }
        }
    }

    override suspend fun triggerBackgroundParsing(feedItemId: String, url: String): ParsingResult {
        logger.d { "Triggering background parsing for: $url (feedItemId: $feedItemId)" }
        // On Desktop, background parsing is the same as immediate parsing
        // The dispatcher handles the threading
        return triggerImmediateParsing(feedItemId, url)
    }
}
