package com.prof18.feedflow.shared.domain.feed

import io.ktor.http.URLProtocol
import io.ktor.http.Url

internal data class YouTubeChannelUrl(
    val pageUrl: String,
    val channelId: String?,
) {
    companion object {
        private val channelIdPattern = Regex("UC[A-Za-z0-9_-]{22}")
        private val channelTabs = setOf("videos", "shorts", "streams", "featured", "playlists", "community", "about")
        private val channelHosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com")

        fun parse(value: String): YouTubeChannelUrl? {
            val url = parseYouTubeUrl(value) ?: return null
            val segments = url.encodedPath.trimEnd('/').removePrefix("/").split('/')
            val channelPathSize = if (segments.first().startsWith("@")) 1 else 2
            if (segments.size !in channelPathSize..channelPathSize + 1 ||
                (segments.size > channelPathSize && segments.last() !in channelTabs)
            ) {
                return null
            }
            val channelId = when (segments.first()) {
                "channel" -> segments[1].takeIf(channelIdPattern::matches) ?: return null
                "c", "user" -> {
                    if (segments[1].isBlank()) return null
                    null
                }
                else -> {
                    if (!segments.first().startsWith("@") || segments.first().length == 1) return null
                    null
                }
            }
            val path = segments.take(channelPathSize).joinToString("/")
            return YouTubeChannelUrl("https://www.youtube.com/$path", channelId)
        }

        fun feedUrl(channelId: String): String =
            "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"

        fun normalizeFeedUrl(value: String): String? {
            val url = parseYouTubeUrl(value) ?: return null
            if (url.encodedPath != "/feeds/videos.xml") return null
            val id = url.parameters["channel_id"]?.takeIf(channelIdPattern::matches) ?: return null
            return feedUrl(id)
        }

        private fun parseYouTubeUrl(value: String): Url? {
            val url = runCatching { Url(value) }.getOrNull() ?: return null
            return url.takeIf {
                it.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS) &&
                    it.host.lowercase() in channelHosts && it.user == null && it.password == null
            }
        }
    }
}
