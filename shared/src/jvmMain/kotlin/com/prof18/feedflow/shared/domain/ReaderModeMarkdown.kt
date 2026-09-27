package com.prof18.feedflow.shared.domain

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
