package com.prof18.feedflow.core.utils

object ContentImageUrlExtractor {

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
     */
    fun extractImageUrl(content: String?): String? {
        return try {
            content
                ?.replace("&amp;amp;", "&amp;")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.let(::decodeNumericEntities)
                ?.let { imageUrlRegex.find(it) }
                ?.value
                ?.trim()
                ?.takeIf { url -> !url.contains(EMOJI_WEBSITE) && !url.contains("/smilies/") }
        } catch (_: Throwable) {
            // On iOS the regex could fail with too much recursion on some contents
            null
        }
    }

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
