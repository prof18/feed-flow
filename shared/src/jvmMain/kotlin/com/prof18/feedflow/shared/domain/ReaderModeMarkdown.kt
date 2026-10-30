package com.prof18.feedflow.shared.domain

import com.prof18.feedflow.core.utils.ContentDirection
import com.prof18.feedflow.core.utils.ContentDirectionDetector

/**
 * Builds the Markdown document shown by the Desktop reader: the Markdown counterpart of
 * [getReaderModeStyledHtml]. [imageUrl] is added as a hero only when the content does not
 * already open with an image.
 */
fun buildReaderModeMarkdown(
    content: String,
    title: String?,
    imageUrl: String?,
    siteName: String?,
): String = buildString {
    if (!title.isNullOrBlank()) {
        appendLine("# $title")
        appendLine()
    }
    if (!siteName.isNullOrBlank()) {
        appendLine("**$siteName**")
        appendLine()
    }
    if (!imageUrl.isNullOrBlank() && !hasLeadingMarkdownImage(content)) {
        appendLine("![]($imageUrl)")
        appendLine()
    }
    append(content)
}

private const val LEADING_IMAGE_SCAN_WINDOW = 1000
private val leadingImageRegex = Regex(
    pattern = "!\\[[^]]*]\\([^)]+\\)|<img\\b",
    option = RegexOption.IGNORE_CASE,
)

private fun hasLeadingMarkdownImage(content: String): Boolean =
    leadingImageRegex.containsMatchIn(content.take(LEADING_IMAGE_SCAN_WINDOW))

/**
 * Resolves the reading direction of a Desktop reader article, the Markdown counterpart of the
 * `dir='auto'` that [getReaderModeStyledHtml] puts on the web reader: the first strong character
 * of the title, site name and body decides. Image and link destinations, HTML tags and bare URLs
 * are skipped, so a body opening with a hero image is still read from its first word.
 */
fun readerModeContentDirection(
    title: String?,
    siteName: String?,
    markdown: String?,
): ContentDirection? = ContentDirectionDetector.detect(
    listOfNotNull(title, siteName, markdown?.take(DIRECTION_SCAN_WINDOW)?.withoutMarkupForDirection()),
)

private fun String.withoutMarkupForDirection(): String = directionMarkupRegexes.fold(this) { text, regex ->
    regex.replace(text, " ")
}

private const val DIRECTION_SCAN_WINDOW = 8000

// A link destination as Klead writes it: parentheses escaped, wrapped in <> when it has spaces,
// optionally followed by a quoted title with escaped quotes.
private const val MARKDOWN_DESTINATION =
    """\((?:<(?:\\.|[^>\\])*>|(?:\\.|[^)\\\s])*)(?:\s+"(?:\\.|[^"\\])*")?\)"""
private val directionMarkupRegexes = listOf(
    Regex("""!\[[^]]*]$MARKDOWN_DESTINATION"""),
    Regex("""]$MARKDOWN_DESTINATION"""),
    Regex("<[^>]*>"),
    Regex("https?://\\S+", RegexOption.IGNORE_CASE),
)
