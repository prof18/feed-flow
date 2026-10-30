package com.prof18.feedflow.core.model

import com.prof18.feedflow.core.utils.HttpHostValidator
import io.ktor.http.URLBuilder

/** Validates RSS enclosure URLs without changing their signed query strings. */
object AudioEnclosure {

    /** Internal reader-link token consumed by platform reader callbacks. */
    const val ACTION_URL = "feedflow-audio://episode/open"
    const val PLAY_ACTION_URL = "feedflow-audio://episode/play"

    fun validatedUrl(url: String?): String? {
        val trimmed = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }

        val authorityStart = trimmed.indexOf("://") + AUTHORITY_PREFIX_LENGTH
        val rawAuthority = trimmed.substring(authorityStart).substringBeforeAny('/', '?', '#')
        if (rawAuthority.isBlank()) return null

        val builder = runCatching { URLBuilder(trimmed) }.getOrNull() ?: return null
        val host = builder.host
        if (host.isBlank() || !HttpHostValidator.isSafeForHttpClient(host)) return null

        return runCatching { builder.build() }.getOrNull()?.let { trimmed }
    }

    fun audioUrlOrNull(url: String?, mimeType: String?): String? {
        val validatedUrl = validatedUrl(url) ?: return null
        val builder = runCatching { URLBuilder(validatedUrl) }.getOrNull() ?: return null
        val parsedUrl = runCatching { builder.build() }.getOrNull() ?: return null
        val normalizedMime = mimeType
            ?.trim()
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()

        if (normalizedMime.startsWith(AUDIO_MIME_PREFIX)) return validatedUrl
        if (normalizedMime.isNotEmpty() && normalizedMime !in unknownMimeTypes) return null

        val extension = parsedUrl.encodedPath
            .substringAfterLast('/')
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return validatedUrl.takeIf { extension in audioExtensions }
    }

    private fun String.substringBeforeAny(vararg delimiters: Char): String {
        val end = indexOfAny(delimiters).takeIf { it >= 0 } ?: length
        return substring(0, end)
    }

    private val audioExtensions = setOf(
        "mp3",
        "m4a",
        "m4b",
        "aac",
        "ogg",
        "oga",
        "opus",
        "wav",
        "flac",
    )

    private val unknownMimeTypes = setOf(
        "application/octet-stream",
        "binary/octet-stream",
        "application/ogg",
    )

    private const val AUDIO_MIME_PREFIX = "audio/"
    private const val AUTHORITY_PREFIX_LENGTH = 3
}
