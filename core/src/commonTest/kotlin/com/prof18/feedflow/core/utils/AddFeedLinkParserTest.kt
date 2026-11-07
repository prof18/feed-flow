package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddFeedLinkParserTest {

    @Test
    fun `app link with encoded url`() {
        assertEquals(
            "https://example.com/feed.xml",
            AddFeedLinkParser.parse("feedflow://add?url=https%3A%2F%2Fexample.com%2Ffeed.xml"),
        )
    }

    @Test
    fun `app link keeps the feed query`() {
        assertEquals(
            "https://example.com/rss?format=atom&a=b",
            AddFeedLinkParser.parse("feedflow://add?url=https%3A%2F%2Fexample.com%2Frss%3Fformat%3Datom%26a%3Db"),
        )
    }

    @Test
    fun `app link with trailing slash`() {
        assertEquals(
            "https://example.com/feed",
            AddFeedLinkParser.parse("feedflow://add/?url=https%3A%2F%2Fexample.com%2Ffeed"),
        )
    }

    @Test
    fun `app link scheme and host are case-insensitive`() {
        assertEquals(
            "https://example.com/feed",
            AddFeedLinkParser.parse("FeedFlow://ADD?url=https%3A%2F%2Fexample.com%2Ffeed"),
        )
    }

    @Test
    fun `app link without url is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://add"))
    }

    @Test
    fun `app link with other host is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://feed/123"))
    }

    @Test
    fun `app link with extra path is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://add/more?url=https%3A%2F%2Fexample.com%2Ffeed"))
    }

    @Test
    fun `app link with javascript url is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://add?url=javascript%3Aalert(1)"))
    }

    @Test
    fun `app link with file url is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://add?url=file%3A%2F%2F%2Fetc%2Fpasswd"))
    }

    @Test
    fun `app link without scheme in url is rejected`() {
        assertNull(AddFeedLinkParser.parse("feedflow://add?url=example.com%2Ffeed"))
    }

    @Test
    fun `feed scheme becomes https`() {
        assertEquals("https://example.com/feed.xml", AddFeedLinkParser.parse("feed://example.com/feed.xml"))
    }

    @Test
    fun `feeds scheme becomes https`() {
        assertEquals("https://example.com/feed.xml", AddFeedLinkParser.parse("feeds://example.com/feed.xml"))
    }

    @Test
    fun `feed scheme wrapping https`() {
        assertEquals("https://example.com/feed.xml", AddFeedLinkParser.parse("feed:https://example.com/feed.xml"))
    }

    @Test
    fun `feed scheme wrapping http keeps http`() {
        assertEquals("http://example.com/feed.xml", AddFeedLinkParser.parse("feed:http://example.com/feed.xml"))
    }

    @Test
    fun `feed scheme with doubled scheme`() {
        assertEquals("https://example.com/feed.xml", AddFeedLinkParser.parse("feed://https://example.com/feed.xml"))
    }

    @Test
    fun `feed scheme without slashes or http is rejected`() {
        assertNull(AddFeedLinkParser.parse("feed:example.com/feed.xml"))
    }

    @Test
    fun `web link`() {
        assertEquals(
            "https://example.com/feed.xml",
            AddFeedLinkParser.parse("https://www.feedflow.dev/add/?url=https%3A%2F%2Fexample.com%2Ffeed.xml"),
        )
    }

    @Test
    fun `web link without trailing slash`() {
        assertEquals(
            "https://example.com/feed.xml",
            AddFeedLinkParser.parse("https://www.feedflow.dev/add?url=https%3A%2F%2Fexample.com%2Ffeed.xml"),
        )
    }

    @Test
    fun `web link on apex domain is rejected`() {
        assertNull(AddFeedLinkParser.parse("https://feedflow.dev/add/?url=https%3A%2F%2Fexample.com%2Ffeed.xml"))
    }

    @Test
    fun `web link on other path is rejected`() {
        assertNull(AddFeedLinkParser.parse("https://www.feedflow.dev/faq/?url=https%3A%2F%2Fexample.com%2Ffeed.xml"))
    }

    @Test
    fun `web link on other host is rejected`() {
        assertNull(AddFeedLinkParser.parse("https://evil.example/add/?url=https%3A%2F%2Fexample.com%2Ffeed.xml"))
    }

    @Test
    fun `plain https feed url is rejected`() {
        assertNull(AddFeedLinkParser.parse("https://example.com/feed.xml"))
    }

    @Test
    fun `unsafe host is rejected`() {
        assertNull(AddFeedLinkParser.parse("feed://exa%zzmple.com/feed"))
    }

    @Test
    fun `blank input is rejected`() {
        assertNull(AddFeedLinkParser.parse("   "))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals("https://example.com/feed.xml", AddFeedLinkParser.parse("  feed://example.com/feed.xml  "))
    }

    @Test
    fun `too long input is rejected`() {
        assertNull(AddFeedLinkParser.parse("feed://example.com/" + "a".repeat(2048)))
    }
}
