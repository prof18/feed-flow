package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.ArticleContentParser
import com.prof18.klead.Klead
import com.prof18.klead.KleadOptions
import com.prof18.klead.KleadOutput
import kotlinx.coroutines.CancellationException

internal class KleadArticleContentParser(
    private val contentFormat: KleadOutput,
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
) : ArticleContentParser {

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
                options = KleadOptions(outputs = setOf(contentFormat)),
            ).content.let { kleadContent ->
                when (contentFormat) {
                    KleadOutput.HTML -> kleadContent.requireHtml()
                    KleadOutput.MARKDOWN -> kleadContent.requireMarkdown()
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

    override suspend fun prepareFeedContent(html: String, baseUrl: String?): String = when (contentFormat) {
        KleadOutput.HTML -> html
        KleadOutput.MARKDOWN -> convertFeedHtmlToMarkdown(html, baseUrl) ?: html
    }

    private suspend fun convertFeedHtmlToMarkdown(html: String, baseUrl: String?): String? = try {
        Klead.parseHtml(
            html = "<html><body><article>$html</article></body></html>",
            url = baseUrl?.takeIf { it.isNotBlank() } ?: FALLBACK_BASE_URL,
            options = KleadOptions(outputs = setOf(KleadOutput.MARKDOWN)),
        ).content.requireMarkdown().trim().takeIf { it.isNotEmpty() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        logger.d(e) { "Error converting feed content to markdown with Klead" }
        null
    }
}

private fun String.visibleLength(contentFormat: KleadOutput): Int {
    val visibleText = when (contentFormat) {
        KleadOutput.HTML -> replace(HTML_TAG_REGEX, " ")
        KleadOutput.MARKDOWN -> replace(MARKDOWN_IMAGE_REGEX, " ").replace(MARKDOWN_LINK_REGEX, "$1")
    }
    return visibleText.replace(WHITESPACE_REGEX, " ").trim().length
}

private const val MIN_CONTENT_LENGTH = 200
private const val FALLBACK_BASE_URL = "https://localhost/"
private val HTML_TAG_REGEX = Regex("<[^>]*>")
private val MARKDOWN_IMAGE_REGEX = Regex("!\\[[^]]*]\\([^)]*\\)")
private val MARKDOWN_LINK_REGEX = Regex("\\[([^]]*)]\\([^)]*\\)")
private val WHITESPACE_REGEX = Regex("\\s+")
