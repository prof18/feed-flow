package com.prof18.feedflow.core.utils

import io.ktor.http.Url

/**
 * Turns an incoming "add this feed" link into the feed URL to pre-fill, or null.
 * Any website or app can fire these links, so only http(s) feed URLs come out.
 */
object AddFeedLinkParser {

    fun parse(link: String): String? {
        val trimmed = link.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LINK_LENGTH) return null
        val separator = trimmed.indexOf(':')
        if (separator <= 0) return null
        val scheme = trimmed.substring(0, separator).lowercase()
        val candidate = when (scheme) {
            in FEED_SCHEMES -> candidateFromFeedScheme(trimmed.substring(separator + 1))
            APP_SCHEME -> candidateFromAppLink(trimmed)
            HTTPS_SCHEME -> candidateFromWebLink(trimmed)
            else -> null
        } ?: return null
        return validated(candidate)
    }

    private fun candidateFromFeedScheme(rest: String): String? = when {
        rest.startsWith("//http://", ignoreCase = true) ||
            rest.startsWith("//https://", ignoreCase = true) -> rest.removePrefix("//")
        rest.startsWith("//") -> "https:$rest"
        rest.hasHttpPrefix() -> rest
        else -> null
    }

    private fun candidateFromAppLink(link: String): String? {
        val url = runCatching { Url(link) }.getOrNull() ?: return null
        if (!url.host.equals(ADD_HOST, ignoreCase = true)) return null
        if (url.encodedPath !in APP_ADD_PATHS) return null
        return url.parameters[URL_PARAM]
    }

    private fun candidateFromWebLink(link: String): String? {
        val url = runCatching { Url(link) }.getOrNull() ?: return null
        if (!url.host.equals(WEB_HOST, ignoreCase = true)) return null
        if (url.encodedPath !in WEB_ADD_PATHS) return null
        return url.parameters[URL_PARAM]
    }

    private fun validated(candidate: String): String? {
        val trimmed = candidate.trim()
        if (trimmed.length > MAX_LINK_LENGTH) return null
        if (!trimmed.hasHttpPrefix()) return null
        val url = runCatching { Url(trimmed) }.getOrNull() ?: return null
        if (url.protocol.name != "http" && url.protocol.name != HTTPS_SCHEME) return null
        if (url.host.isBlank()) return null
        if (!HttpHostValidator.isUrlSafeForHttpClient(trimmed)) return null
        return trimmed
    }

    private fun String.hasHttpPrefix(): Boolean =
        startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true)

    private const val MAX_LINK_LENGTH = 2048
    private const val APP_SCHEME = "feedflow"
    private const val HTTPS_SCHEME = "https"
    private const val ADD_HOST = "add"
    private const val URL_PARAM = "url"
    private const val WEB_HOST = "www.feedflow.dev"
    private val FEED_SCHEMES = setOf("feed", "feeds")
    private val WEB_ADD_PATHS = setOf("/add", "/add/")
    private val APP_ADD_PATHS = setOf("", "/")
}
