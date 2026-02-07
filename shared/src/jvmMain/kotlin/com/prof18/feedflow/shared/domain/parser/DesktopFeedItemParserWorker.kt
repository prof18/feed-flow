package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.presentation.MarkdownToHtmlConverter
import kotlinx.coroutines.withContext
import net.dankito.readability4j.extended.Readability4JExtended
import org.jsoup.Jsoup

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
                val html = htmlRetriever.retrieveHtml(url) ?: return@withContext ParsingResult.Error

                val readability4J = Readability4JExtended(url, html)
                val article = runCatching {
                    readability4J.parse()
                }.getOrNull() ?: return@withContext ParsingResult.Error

                val content = article.contentWithDocumentsCharsetOrUtf8
                    ?.replace(Regex("https?://.*?placeholder\\.png"), "")

                if (content == null) {
                    logger.d { "No content extracted for: $url" }
                    return@withContext ParsingResult.Error
                }

                val plainText = article.textContent ?: ""
                if (plainText.length < MIN_CONTENT_LENGTH) {
                    logger.d { "Content too short (${plainText.length} chars), rejecting: $url" }
                    return@withContext ParsingResult.Error
                }

                // Convert to styled HTML and then to markdown for Desktop
                val title = article.title
                val cleanedContent = stripDuplicateTitles(content, title)
                val styledHtml = getReaderModeStyledHtml(
                    colors = null,
                    content = cleanedContent,
                    fontSize = settingsRepository.getReaderModeFontSize(),
                    title = title,
                )
                val markdown = markdownToHtmlConverter.convertToMarkdown(styledHtml)
                    .replace(Regex("""\s*\{#[^}]+}"""), "")

                if (settingsRepository.isSaveItemContentOnOpenEnabled()) {
                    feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, markdown)
                    logger.d { "Successfully parsed and cached content for: $url (feedItemId: $feedItemId)" }
                }

                ParsingResult.Success(
                    htmlContent = markdown,
                    title = article.title,
                    siteName = null,
                )
            } catch (e: Throwable) {
                logger.d(e) { "Error parsing content for: $url" }
                ParsingResult.Error
            }
        }
    }

    private fun stripDuplicateTitles(content: String, title: String?): String {
        if (title.isNullOrBlank()) return content
        val normalizedTitle = title.trim().lowercase()
        val doc = Jsoup.parse(content)
        doc.select("h1, h2").forEach { element ->
            if (element.text().trim().lowercase() == normalizedTitle) {
                element.remove()
            }
        }
        return doc.body().html()
    }

    private companion object {
        private const val MIN_CONTENT_LENGTH = 200
    }
}
