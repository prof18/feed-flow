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
        private val playlistIdPattern = Regex("[A-Za-z0-9_-]+")

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
            val id = channelIdFromFeedUrl(value) ?: return null
            return feedUrl(id)
        }

        fun channelIdFromFeedUrl(value: String): String? {
            val url = parseYouTubeUrl(value) ?: return null
            if (url.encodedPath != "/feeds/videos.xml") return null
            if (url.parameters.contains("playlist_id")) return null
            return url.parameters.getAll("channel_id")?.singleOrNull()?.takeIf(channelIdPattern::matches)
        }

        fun normalizePlaylistUrl(value: String): String? {
            val url = parseYouTubeUrl(value) ?: return null
            val key = when (url.encodedPath) {
                "/playlist", "/watch" -> "list"
                "/feeds/videos.xml" -> "playlist_id"
                else -> return null
            }
            if (url.parameters.contains("channel_id")) return null
            val id = url.parameters.getAll(key)?.singleOrNull()?.takeIf(playlistIdPattern::matches) ?: return null
            if (id.startsWith("RD") || id in setOf("WL", "LL")) return null
            return "https://www.youtube.com/feeds/videos.xml?playlist_id=$id"
        }

        fun noShortsFeedUrl(channelId: String): String {
            require(channelIdPattern.matches(channelId))
            return "https://www.youtube.com/feeds/videos.xml?playlist_id=UULF${channelId.drop(2)}"
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
