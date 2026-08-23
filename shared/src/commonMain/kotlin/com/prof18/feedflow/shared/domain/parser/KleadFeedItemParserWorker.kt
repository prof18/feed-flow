package com.prof18.feedflow.shared.domain.parser

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ParsingResult
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.domain.HtmlRetriever
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.feeditem.FeedItemParserWorker
import com.prof18.klead.Klead
import com.prof18.klead.KleadOptions
import com.prof18.klead.KleadOutput
import com.prof18.klead.KleadResult
import kotlinx.coroutines.CancellationException

internal class KleadFeedItemParserWorker(
    private val contentFormat: KleadContentFormat,
    private val htmlRetriever: HtmlRetriever,
    private val logger: Logger,
    private val feedItemContentFileHandler: FeedItemContentFileHandler,
    private val settingsRepository: SettingsRepository,
) : FeedItemParserWorker {

    override suspend fun parse(feedItemId: String, url: String, imageUrl: String?): ParsingResult {
        logger.d { "Parsing with Klead: $url (feedItemId: $feedItemId)" }

        return try {
            val html = htmlRetriever.retrieveHtml(url)
            if (html == null) {
                logger.d { "Failed to retrieve HTML for Klead: $url" }
                return ParsingResult.Error
            }

            val parseResult = Klead.parseHtml(
                html = html,
                url = url,
                options = KleadOptions(
                    outputs = setOf(contentFormat.toKleadOutput()),
                ),
            )
            val rawContent = parseResult.contentFor(contentFormat).trim()
            val visibleLength = rawContent.visibleLength(contentFormat)
            if (visibleLength < MIN_CONTENT_LENGTH) {
                logger.d { "Klead content too short ($visibleLength chars), rejecting: $url" }
                return ParsingResult.Error
            }

            val content = parseResult.buildReaderContent(
                contentFormat = contentFormat,
                rawContent = rawContent,
                imageUrl = imageUrl,
            )
            if (settingsRepository.isSaveItemContentOnOpenEnabled()) {
                feedItemContentFileHandler.saveFeedItemContentToFile(feedItemId, content)
                logger.d { "Successfully parsed and cached with Klead: $url" }
            }

            ParsingResult.Success(
                htmlContent = content,
                title = parseResult.metadata.title,
                siteName = parseResult.metadata.site,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.d(e) { "Error parsing with Klead: $url" }
            ParsingResult.Error
        }
    }
}

private fun KleadContentFormat.toKleadOutput(): KleadOutput = when (this) {
    KleadContentFormat.HTML -> KleadOutput.HTML
    KleadContentFormat.MARKDOWN -> KleadOutput.MARKDOWN
}

private fun KleadResult.contentFor(contentFormat: KleadContentFormat): String = when (contentFormat) {
    KleadContentFormat.HTML -> content.requireHtml()
    KleadContentFormat.MARKDOWN -> content.requireMarkdown()
}

private fun String.visibleLength(contentFormat: KleadContentFormat): Int = when (contentFormat) {
    KleadContentFormat.HTML -> replace(HTML_TAG_REGEX, " ").replace(WHITESPACE_REGEX, " ").trim().length
    KleadContentFormat.MARKDOWN -> length
}

private fun KleadResult.buildReaderContent(
    contentFormat: KleadContentFormat,
    rawContent: String,
    imageUrl: String?,
): String = when (contentFormat) {
    KleadContentFormat.HTML -> buildString {
        val title = metadata.title
        if (!title.isNullOrBlank()) {
            append("<h1>")
            append(title.escapeHtml())
            appendLine("</h1>")
        }
        val site = metadata.site
        if (!site.isNullOrBlank()) {
            append("<h4>")
            append(site.escapeHtml())
            appendLine("</h4>")
        }
        append(rawContent)
    }.trim()

    KleadContentFormat.MARKDOWN -> buildString {
        val title = metadata.title
        if (!title.isNullOrBlank()) {
            appendLine("# $title")
            appendLine()
        }
        val site = metadata.site
        if (!site.isNullOrBlank()) {
            appendLine("**$site**")
            appendLine()
        }
        if (!imageUrl.isNullOrBlank() && !rawContent.hasReaderImage(imageUrl)) {
            appendLine("![]($imageUrl)")
            appendLine()
        }
        append(rawContent)
    }.trim()
}

private fun String.hasReaderImage(imageUrl: String): Boolean =
    trimStart().startsWith("![") || hasMatchingImage(imageUrl)

private fun String.hasMatchingImage(imageUrl: String): Boolean {
    val identity = imageUrl.imageIdentity() ?: return false
    return identity in this
}

private fun String.imageIdentity(): String? {
    val fileName = substringBefore('?').substringAfterLast('/')
    if (fileName.isBlank()) return null
    return fileName
        .removeSuffix(".webp")
        .substringBeforeLast('.', missingDelimiterValue = fileName)
        .replace(IMAGE_SIZE_SUFFIX_REGEX, "")
        .takeIf { it.length >= MIN_IMAGE_IDENTITY_LENGTH }
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

private const val MIN_CONTENT_LENGTH = 200
private const val MIN_IMAGE_IDENTITY_LENGTH = 8
private val HTML_TAG_REGEX = Regex("<[^>]*>")
private val WHITESPACE_REGEX = Regex("\\s+")
private val IMAGE_SIZE_SUFFIX_REGEX = Regex("-\\d+(?:-\\d+)?$")
