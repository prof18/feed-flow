package com.prof18.feedflow.shared.domain

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

object ReaderContentLanguage {

    private const val LANGUAGE_ATTRIBUTE = "data-feedflow-language"

    fun fromHtml(html: String): String? {
        val document = runCatching { Ksoup.parse(html, "") }.getOrNull() ?: return null

        document.select("[$LANGUAGE_ATTRIBUTE]")
            .firstOrNull()
            ?.let { normalize(it.attr("lang")) }
            ?.let { return it }

        val articleLanguages = document.select("article[lang], article[xml:lang], main[lang], main[xml:lang]")
            .filter { element -> element.parents().none { it.tagName() in ignoredLanguageContainers } }
            .mapNotNull(::declaredLanguage)
            .distinct()
        if (articleLanguages.size == 1) return articleLanguages.single()

        declaredLanguage(document.selectFirst("html"))?.let { return it }

        declaredLanguage(document.selectFirst("body"))?.let { return it }

        val root = document.body().children().singleOrNull()
            ?.takeUnless { it.tagName() in ignoredLanguageContainers }
        declaredLanguage(root)?.let { return it }

        val contentLanguage = document.head().select("meta[http-equiv]")
            .firstOrNull { it.attr("http-equiv").equals("content-language", ignoreCase = true) }
            ?.attr("content")
        normalize(contentLanguage)?.let { return it }

        val locale = document.head().select("meta[property]")
            .firstOrNull { it.attr("property").equals("og:locale", ignoreCase = true) }
            ?.attr("content")
            ?.replace('_', '-')
        return normalize(locale)
    }

    fun preserve(content: String, languageTag: String?): String {
        val language = normalize(languageTag) ?: return content
        return "<div data-feedflow-language lang=\"$language\">$content</div>"
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

    private fun declaredLanguage(element: Element?): String? {
        if (element == null) return null
        return normalize(element.attr("lang")) ?: normalize(element.attr("xml:lang"))
    }

    private val ignoredLanguageContainers = setOf("nav", "header", "footer", "aside", "blockquote", "code", "pre")

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
