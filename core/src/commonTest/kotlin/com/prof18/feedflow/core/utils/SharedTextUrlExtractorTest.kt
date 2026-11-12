package com.prof18.feedflow.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedTextUrlExtractorTest {

    @Test
    fun `null and empty text have no url`() {
        assertNull(SharedTextUrlExtractor.firstHttpUrl(null))
        assertNull(SharedTextUrlExtractor.firstHttpUrl(""))
    }

    @Test
    fun `text without a link has no url`() {
        assertNull(SharedTextUrlExtractor.firstHttpUrl("no link here"))
    }

    @Test
    fun `a bare url is returned as is`() {
        assertEquals(
            "https://example.com/feed.xml",
            SharedTextUrlExtractor.firstHttpUrl("https://example.com/feed.xml"),
        )
    }

    @Test
    fun `the url is taken out of surrounding prose`() {
        assertEquals(
            "https://example.com/feed.xml",
            SharedTextUrlExtractor.firstHttpUrl("Read this: https://example.com/feed.xml cool"),
        )
    }

    @Test
    fun `trailing sentence punctuation and an unmatched bracket are trimmed`() {
        assertEquals(
            "http://example.com/rss",
            SharedTextUrlExtractor.firstHttpUrl("(see http://example.com/rss)."),
        )
    }

    @Test
    fun `an html href is decoded`() {
        assertEquals(
            "https://example.com/feed?a=1&b=2",
            SharedTextUrlExtractor.firstHttpUrl("<a href=\"https://example.com/feed?a=1&amp;b=2\">x</a>"),
        )
    }

    @Test
    fun `non http links are skipped`() {
        assertEquals(
            "https://second.example/feed",
            SharedTextUrlExtractor.firstHttpUrl("ftp://example.com https://second.example/feed"),
        )
    }

    @Test
    fun `the scheme is matched case insensitively`() {
        assertEquals(
            "HTTPS://Example.com/Feed",
            SharedTextUrlExtractor.firstHttpUrl("HTTPS://Example.com/Feed"),
        )
    }

    @Test
    fun `a bare url keeps a trailing exclamation mark`() {
        assertEquals(
            "https://example.com/feed?token=abc!",
            SharedTextUrlExtractor.firstHttpUrl("https://example.com/feed?token=abc!"),
        )
    }

    @Test
    fun `balanced brackets in the url are kept`() {
        assertEquals(
            "https://example.com/rss(topic)",
            SharedTextUrlExtractor.firstHttpUrl("Feed: https://example.com/rss(topic)."),
        )
    }

    @Test
    fun `a quoted href keeps its ending`() {
        assertEquals(
            "https://example.com/feed?token=abc!",
            SharedTextUrlExtractor.firstHttpUrl("<a href=\"https://example.com/feed?token=abc!\">x</a>"),
        )
    }

    @Test
    fun `a bare url keeps an apostrophe`() {
        assertEquals(
            "https://example.com/feeds/o'reilly.xml",
            SharedTextUrlExtractor.firstHttpUrl("  https://example.com/feeds/o'reilly.xml\n"),
        )
    }

    @Test
    fun `a url in prose keeps an apostrophe`() {
        assertEquals(
            "https://example.com/feeds/o'reilly.xml",
            SharedTextUrlExtractor.firstHttpUrl("Read this: https://example.com/feeds/o'reilly.xml"),
        )
    }

    @Test
    fun `a double quoted href keeps an apostrophe`() {
        assertEquals(
            "https://example.com/feeds/o'reilly.xml",
            SharedTextUrlExtractor.firstHttpUrl("<a href=\"https://example.com/feeds/o'reilly.xml\">x</a>"),
        )
    }

    @Test
    fun `a single quoted url ends at the closing quote`() {
        assertEquals(
            "https://example.com/rss",
            SharedTextUrlExtractor.firstHttpUrl("<a href='https://example.com/rss'>x</a>"),
        )
        assertEquals(
            "https://example.com/rss",
            SharedTextUrlExtractor.firstHttpUrl("Subscribe to 'https://example.com/rss' today"),
        )
    }

    @Test
    fun `an overlong url is rejected`() {
        assertNull(SharedTextUrlExtractor.firstHttpUrl("https://example.com/" + "a".repeat(2100)))
    }
}
