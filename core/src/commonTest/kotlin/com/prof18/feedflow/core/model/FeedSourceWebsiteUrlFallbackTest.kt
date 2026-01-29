package com.prof18.feedflow.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedSourceWebsiteUrlFallbackTest {

    @Test
    fun `returns website url when available`() {
        val feedSource = createFeedSource(
            url = "https://example.com/feed.xml",
            websiteUrl = "https://example.com",
        )

        assertEquals("https://example.com", feedSource.websiteUrlFallback())
    }

    @Test
    fun `derives base url from https feed url`() {
        val feedSource = createFeedSource(
            url = "https://example.com/news/feed.xml",
            websiteUrl = null,
        )

        assertEquals("https://example.com", feedSource.websiteUrlFallback())
    }

    @Test
    fun `derives base url from http feed url`() {
        val feedSource = createFeedSource(
            url = "http://example.com/rss",
            websiteUrl = null,
        )

        assertEquals("http://example.com", feedSource.websiteUrlFallback())
    }

    @Test
    fun `derives base url from feed url without scheme`() {
        val feedSource = createFeedSource(
            url = "example.com/rss",
            websiteUrl = null,
        )

        assertEquals("https://example.com", feedSource.websiteUrlFallback())
    }

    @Test
    fun `keeps port when deriving base url`() {
        val feedSource = createFeedSource(
            url = "https://example.com:8443/rss",
            websiteUrl = null,
        )

        assertEquals("https://example.com:8443", feedSource.websiteUrlFallback())
    }

    @Test
    fun `returns null when url is blank and website url missing`() {
        val feedSource = createFeedSource(
            url = "   ",
            websiteUrl = null,
        )

        assertNull(feedSource.websiteUrlFallback())
    }

    private fun createFeedSource(
        url: String,
        websiteUrl: String?,
    ) = FeedSource(
        id = "source-id",
        url = url,
        title = "Feed",
        category = null,
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = websiteUrl,
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )
}
