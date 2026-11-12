package com.prof18.feedflow.core.utils

/** Finds the link to add in text another app shared, which can be prose or HTML. */
object SharedTextUrlExtractor {

    // Apostrophes are valid in URLs (o'reilly.xml), so only whitespace, double quotes and angle brackets end a match.
    private val httpUrl = Regex("""https?://[^\s"<>]+""", RegexOption.IGNORE_CASE)

    // A share that is only a URL is taken whole: quotes and brackets only delimit URLs inside prose or HTML.
    private val bareHttpUrl = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
    private const val PROSE_TRAILING_PUNCTUATION = ".,;:!?'"
    private const val MAX_URL_LENGTH = 2048
    private val closingToOpening = mapOf(')' to '(', ']' to '[', '}' to '{')

    fun firstHttpUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val decoded = text.replace("&amp;", "&")
        val trimmed = decoded.trim()
        if (bareHttpUrl.matches(trimmed)) return trimmed.takeIf { it.length <= MAX_URL_LENGTH }
        val found = httpUrl.find(decoded) ?: return null
        val match = found.value
        if (match.length > MAX_URL_LENGTH) return null
        return when (decoded.getOrNull(found.range.first - 1)) {
            '"' -> match
            '\'' -> match.substringBefore('\'')
            else -> trimProse(match)
        }.takeIf { it.isNotEmpty() }
    }

    private fun trimProse(url: String): String {
        var result = url
        while (true) {
            val last = result.lastOrNull() ?: return result
            val opening = closingToOpening[last]
            val unbalanced = opening != null &&
                result.count { it == opening } < result.count { it == last }
            if (last !in PROSE_TRAILING_PUNCTUATION && !unbalanced) return result
            result = result.dropLast(1)
        }
    }
}
