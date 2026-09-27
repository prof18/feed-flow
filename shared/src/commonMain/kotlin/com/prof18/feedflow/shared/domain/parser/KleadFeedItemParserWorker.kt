package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.klead.Klead
import com.prof18.klead.KleadOptions
import com.prof18.klead.KleadOutput
import kotlinx.coroutines.CancellationException

internal class KleadFeedItemParserWorker(
    private val contentFormat: KleadContentFormat,
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
) : FeedItemParserWorker {

    override suspend fun parse(url: String): String? {
        logger.d { "Parsing with Klead: $url" }

        return try {
            val html = htmlRetriever.retrieveHtml(url)
            if (html == null) {
                logger.d { "Failed to retrieve HTML for Klead: $url" }
                return null
            }

            val content = Klead.parseHtml(
                html = html,
                url = url,
                options = KleadOptions(outputs = setOf(contentFormat.toKleadOutput())),
            ).content.let { kleadContent ->
                when (contentFormat) {
                    KleadContentFormat.HTML -> kleadContent.requireHtml()
                    KleadContentFormat.MARKDOWN -> kleadContent.requireMarkdown()
                }
            }.trim()

            val visibleLength = content.visibleLength(contentFormat)
            if (visibleLength < MIN_CONTENT_LENGTH) {
                logger.d { "Klead content too short ($visibleLength chars), rejecting: $url" }
                return null
            }

            logger.d { "Successfully parsed with Klead: $url" }
            content
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.d(e) { "Error parsing with Klead: $url" }
            null
        }
    }
}

private fun KleadContentFormat.toKleadOutput(): KleadOutput = when (this) {
    KleadContentFormat.HTML -> KleadOutput.HTML
    KleadContentFormat.MARKDOWN -> KleadOutput.MARKDOWN
}

private fun String.visibleLength(contentFormat: KleadContentFormat): Int {
    val visibleText = when (contentFormat) {
        KleadContentFormat.HTML -> replace(HTML_TAG_REGEX, " ")
        KleadContentFormat.MARKDOWN -> replace(MARKDOWN_IMAGE_REGEX, " ").replace(MARKDOWN_LINK_REGEX, "$1")
    }
    return visibleText.replace(WHITESPACE_REGEX, " ").trim().length
}

private const val MIN_CONTENT_LENGTH = 200
private val HTML_TAG_REGEX = Regex("<[^>]*>")
private val MARKDOWN_IMAGE_REGEX = Regex("!\\[[^]]*]\\([^)]*\\)")
private val MARKDOWN_LINK_REGEX = Regex("\\[([^]]*)]\\([^)]*\\)")
private val WHITESPACE_REGEX = Regex("\\s+")
