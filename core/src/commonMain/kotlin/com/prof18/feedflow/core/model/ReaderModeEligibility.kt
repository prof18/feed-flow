package com.prof18.feedflow.core.model

import com.prof18.feedflow.core.utils.HttpHostValidator
import io.ktor.http.Url

object ReaderModeEligibility {

    fun canOpenReaderMode(url: String): Boolean {
        val parsedUrl = runCatching { Url(url.trim()) }.getOrNull() ?: return false
        if (parsedUrl.protocol.name !in webProtocols) {
            return false
        }

        val host = parsedUrl.host.lowercase()
        if (!HttpHostValidator.isSafeForHttpClient(host) || host.isYouTubeHost() || host.isTelegramHost()) {
            return false
        }

        val extension = parsedUrl.encodedPath
            .lowercase()
            .substringAfterLast('/')
            .substringAfterLast('.', missingDelimiterValue = "")
        if (extension in blockedPathExtensions) {
            return false
        }

        return !hasBlockedQueryHint(parsedUrl)
    }

    private fun hasBlockedQueryHint(url: Url): Boolean =
        url.parameters.names().any { parameterKey ->
            val key = parameterKey.lowercase()
            val values = url.parameters.getAll(parameterKey).orEmpty()
            values.any { parameterValue ->
                val value = parameterValue.lowercase()
                key in pdfQueryKeys && value == PDF_EXTENSION ||
                    key == DOWNLOAD_QUERY_KEY && value == DOWNLOAD_QUERY_VALUE
            }
        }

    private fun String.isYouTubeHost(): Boolean =
        this == "youtu.be" ||
            this == "youtube.com" ||
            endsWith(".youtube.com") ||
            this == "youtube-nocookie.com" ||
            endsWith(".youtube-nocookie.com")

    private fun String.isTelegramHost(): Boolean =
        this == "t.me" ||
            endsWith(".t.me") ||
            this == "telegram.me" ||
            endsWith(".telegram.me") ||
            this == "telegram.dog" ||
            endsWith(".telegram.dog")

    private val webProtocols = setOf(
        "http",
        "https",
    )

    private val pdfQueryKeys = setOf(
        "type",
        "format",
        "output",
        "filetype",
    )

    private const val PDF_EXTENSION = "pdf"
    private const val DOWNLOAD_QUERY_KEY = "download"
    private const val DOWNLOAD_QUERY_VALUE = "1"

    private val blockedPathExtensions = setOf(
        "aac",
        "apk",
        "avi",
        "bin",
        "bz2",
        "dmg",
        "doc",
        "docx",
        "epub",
        "exe",
        "flac",
        "gif",
        "gz",
        "iso",
        "jpeg",
        "jpg",
        "m3u",
        "m3u8",
        "m4a",
        "m4v",
        "mkv",
        "mov",
        "mp3",
        "mp4",
        "ogg",
        "ogv",
        "pdf",
        "png",
        "ppt",
        "pptx",
        "rar",
        "svg",
        "tar",
        "tgz",
        "torrent",
        "wav",
        "webm",
        "webp",
        "woff",
        "woff2",
        "xls",
        "xlsx",
        "zip",
        "7z",
    )
}
