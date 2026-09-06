package com.prof18.feedflow.shared.domain.feed

import com.prof18.feedflow.shared.test.KoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeChannelUrlTest : KoinTestBase() {

    private val channelId = "UCsBjURrPoezykLs9EqgamOA"

    @Test
    fun `parse supports channel URL variants and normalizes page URL`() {
        val cases = mapOf(
            "https://youtube.com/channel/$channelId" to "https://www.youtube.com/channel/$channelId",
            "http://www.youtube.com/channel/$channelId/" to "https://www.youtube.com/channel/$channelId",
            "https://m.youtube.com/channel/$channelId/videos?foo=bar" to "https://www.youtube.com/channel/$channelId",
            "https://www.youtube.com/c/feedflow/about" to "https://www.youtube.com/c/feedflow",
            "https://www.youtube.com/user/feedflow/" to "https://www.youtube.com/user/feedflow",
            "https://www.youtube.com/@feedflow/community" to "https://www.youtube.com/@feedflow",
        )

        cases.forEach { (url, expectedPageUrl) ->
            val result = YouTubeChannelUrl.parse(url)
            assertEquals(expectedPageUrl, result?.pageUrl, url)
            assertEquals(if ("/channel/" in url) channelId else null, result?.channelId, url)
        }
    }

    @Test
    fun `parse accepts all supported channel tabs`() {
        listOf("videos", "shorts", "streams", "featured", "playlists", "community", "about").forEach { tab ->
            assertEquals(
                "https://www.youtube.com/@feedflow",
                YouTubeChannelUrl.parse("https://www.youtube.com/@feedflow/$tab")?.pageUrl,
            )
        }
    }

    @Test
    fun `parse rejects non channel and unsafe URL variants`() {
        listOf(
            "https://www.youtube.com/",
            "https://www.youtube.com/watch?v=abc",
            "https://www.youtube.com/playlist?list=abc",
            "https://www.youtube.com/shorts/abc",
            "https://youtu.be/$channelId",
            "https://youtube.com.evil.example/@feedflow",
            "https://user:pass@youtube.com/@feedflow",
            "ftp://youtube.com/@feedflow",
            "https://www.youtube.com/@",
            "https://www.youtube.com/channel/invalid",
            "https://www.youtube.com/channel/${channelId}x",
            "https://www.youtube.com/channel/UC123/featured",
            "https://www.youtube.com/c/",
            "https://www.youtube.com/user/",
            "https://www.youtube.com/@feedflow/watch",
        ).forEach { assertNull(YouTubeChannelUrl.parse(it), it) }
    }

    @Test
    fun `normalizeFeedUrl accepts canonical RSS URL variants`() {
        listOf(
            "https://youtube.com/feeds/videos.xml?channel_id=$channelId",
            "http://www.youtube.com/feeds/videos.xml?channel_id=$channelId",
            "https://m.youtube.com/feeds/videos.xml?channel_id=$channelId&extra=ignored",
        ).forEach {
            assertEquals(YouTubeChannelUrl.feedUrl(channelId), YouTubeChannelUrl.normalizeFeedUrl(it), it)
        }
    }

    @Test
    fun `normalizeFeedUrl rejects invalid IDs and external lookalike URLs`() {
        listOf(
            "https://www.youtube.com/feeds/videos.xml",
            "https://www.youtube.com/feeds/videos.xml?channel_id=UC123",
            "https://www.youtube.com/feeds/videos.xml?channel_id=${channelId}x",
            "https://youtube.com.evil.example/feeds/videos.xml?channel_id=$channelId",
            "https://user:pass@youtube.com/feeds/videos.xml?channel_id=$channelId",
            "ftp://youtube.com/feeds/videos.xml?channel_id=$channelId",
            "https://www.youtube.com/feed/videos.xml?channel_id=$channelId",
        ).forEach { assertNull(YouTubeChannelUrl.normalizeFeedUrl(it), it) }
    }
}
