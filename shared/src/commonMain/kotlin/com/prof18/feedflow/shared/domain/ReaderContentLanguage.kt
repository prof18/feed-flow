package com.prof18.feedflow.shared.domain

object ReaderContentLanguage {

    private const val CACHE_PREFIX = "<div data-feedflow-language lang=\""

    fun fromCachedContent(content: String): String? {
        if (!content.startsWith(CACHE_PREFIX)) return null
        val languageEnd = content.indexOf("\">", startIndex = CACHE_PREFIX.length)
        if (languageEnd < 0) return null
        return normalize(content.substring(CACHE_PREFIX.length, languageEnd))
    }

    fun preserve(content: String, languageTag: String?): String {
        val language = normalize(languageTag) ?: return content
        return "$CACHE_PREFIX$language\">$content</div>"
    }

    fun normalize(languageTag: String?): String? {
        val tag = languageTag?.trim() ?: return null
        if (!languageTagPattern.matches(tag)) return null
        val parts = tag.split('-')
        if (parts.first().lowercase() in unsupportedLanguages || parts.last().length == 1) return null
        if (parts.drop(1).zipWithNext().any { (left, right) -> left.length == 1 && right.length == 1 }) return null
        return parts.mapIndexed { index, part ->
            if (index == 0) part.lowercase() else canonicalSubtag(part)
        }.joinToString("-")
    }

    private fun canonicalSubtag(part: String): String = when {
        part.length == SCRIPT_SUBTAG_LENGTH && part.all { it.isAsciiLetter() } ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        (part.length == 2 && part.all { it.isAsciiLetter() }) ||
            (part.length == NUMERIC_REGION_SUBTAG_LENGTH && part.all { it.isAsciiDigit() }) -> part.uppercase()
        else -> part.lowercase()
    }

    private const val SCRIPT_SUBTAG_LENGTH = 4
    private const val NUMERIC_REGION_SUBTAG_LENGTH = 3
    private val languageTagPattern = Regex("[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*")
    private val unsupportedLanguages = setOf("und", "mul", "zxx")

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
