package com.prof18.feedflow.core.utils

object ContentImageUrlExtractor {

    private val imageUrlRegex = Regex(
        pattern = """https?://[^\s<>"']+\.(?:jpg|jpeg|png|gif|bmp|webp)(?:\?[^\s<>"']*)?""",
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
                ?.let { imageUrlRegex.find(it) }
                ?.value
                ?.trim()
                ?.takeIf { url -> !url.contains(EMOJI_WEBSITE) && !url.contains("/smilies/") }
        } catch (_: Throwable) {
            // On iOS the regex could fail with too much recursion on some contents
            null
        }
    }

    private const val EMOJI_WEBSITE = "https://s.w.org/images/core/emoji"
}
