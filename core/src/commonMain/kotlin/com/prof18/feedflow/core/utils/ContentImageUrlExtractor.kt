package com.prof18.feedflow.core.utils

object ContentImageUrlExtractor {

    private val imgTagRegex = Regex(
        pattern = """<img\b[^>]*>""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    // The leading delimiter keeps "src" from matching the tail of "data-src", so both attributes
    // stay separate candidates instead of the first one swallowing the second.
    private val imgSourceAttributeRegex = Regex(
        pattern = """[\s"'](?:src|data-src|data-original|data-lazy-src)\s*=\s*""" +
            """(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    private val imageUrlRegex = Regex(
        pattern = """https?://[^\s<>"']+\.(?:jpg|jpeg|png|gif|bmp|webp)(?:\?[^\s<>"']*)?""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    private val numericEntityRegex = Regex(
        pattern = """&#(x?)([0-9a-fA-F]{1,6});""",
        options = setOf(RegexOption.IGNORE_CASE),
    )

    /**
     * Finds the first image url inside the provided HTML content, keeping any query string
     * since some websites use it to request a properly sized image.
     *
     * The source of truth is the `src` of the first usable `<img>` tag: extension-less image
     * endpoints (`.../image-service/images/urn:ard:image:abc?w=432`) are common enough that
     * matching bare urls by file extension misses them entirely. That match is kept as a
     * fallback for content that references an image without wrapping it in a tag.
     */
    fun extractImageUrl(content: String?): String? {
        return try {
            val decoded = content
                ?.replace("&amp;amp;", "&amp;")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.let(::decodeNumericEntities)
                ?: return null

            decoded.firstImageTagSource() ?: decoded.firstBareImageUrl()
        } catch (_: Throwable) {
            // On iOS the regex could fail with too much recursion on some contents
            null
        }
    }

    // Deliberately no `contains("<img")` pre-check: on iOS a case-insensitive substring scan
    // costs about as much as the regex pass it would guard, so it only slows down the common
    // case where the tag is present.
    private fun String.firstImageTagSource(): String? =
        imgTagRegex.findAll(this)
            .flatMap { tag -> imgSourceAttributeRegex.findAll(tag.value) }
            // Only one of the quoting alternatives captures, the others come back empty.
            .mapNotNull { match -> match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } }
            .map { it.trim() }
            .firstOrNull { it.isUsableImageUrl() }

    private fun String.firstBareImageUrl(): String? =
        imageUrlRegex.findAll(this)
            .map { it.value.trim() }
            .firstOrNull { it.isUsableImageUrl() }

    private fun String.isUsableImageUrl(): Boolean =
        // Relative and data: sources can't be resolved without the article base url, and lazy
        // loading placeholders live there, so only absolute http(s) sources are accepted.
        (startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)) &&
            !contains(EMOJI_WEBSITE) &&
            !contains("/smilies/")

    private fun decodeNumericEntities(content: String): String =
        numericEntityRegex.replace(content) { match ->
            val isHex = match.groupValues[1].isNotEmpty()
            val body = match.groupValues[2]
            val codePoint = if (isHex) {
                body.toIntOrNull(radix = 16)
            } else {
                body.takeIf { it.all(Char::isDigit) }?.toIntOrNull()
            }
            val char = codePoint
                ?.takeIf { it in 1..MAX_BMP_CODE_POINT }
                ?.toChar()
                ?.takeIf { !it.isSurrogate() }
            char?.toString() ?: match.value
        }

    private const val MAX_BMP_CODE_POINT = 0xFFFF
    private const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
}
