package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeatureFlags
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.presentation.MarkdownToHtmlConverter
import kotlinx.coroutines.withContext
import net.dankito.readability4j.extended.Readability4JExtended

internal class DesktopFeedItemParserWorker(
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val markdownToHtmlConverter: MarkdownToHtmlConverter,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker {
    override suspend fun parse(feedItemId: String, url: String): ParsingResult {
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
                if (plainText.length < MIN_CONTENT_LENGTH) {
                    logger.w { "Content too short (${plainText.length} chars), rejecting: $url" }
                    return@withContext ParsingResult.Error
                }

                val title = article.title
                val resultContent = if (FeatureFlags.USE_RICH_TEXT_FOR_READER_MODE) {
                    // For RichText mode, return raw HTML content
                    content
                } else {
                    // Convert to styled HTML and then to markdown for Markdown renderer
                    val styledHtml = getReaderModeStyledHtml(
                        colors = null,
                        content = content,
                        fontSize = settingsRepository.getReaderModeFontSize(),
                        title = title,
                    )
                    markdownToHtmlConverter.convertToMarkdown(styledHtml)
                        .replace(Regex("""\s*\{#[^}]+}"""), "")
                }

                if (settingsRepository.isSaveItemContentOnOpenEnabled()) {
                    feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, resultContent)
                    logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }
                }

                ParsingResult.Success(
                    htmlContent = resultContent,
                    title = article.title,
                    siteName = null,
                )
            } catch (e: Throwable) {
                logger.e(e) { "Error parsing content for: $url" }
                ParsingResult.Error
            }
        }
    }

    private companion object {
        private const val MIN_CONTENT_LENGTH = 200
    }
}
