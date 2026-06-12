package com.prof18.feedflow.desktop.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringExtensionsTest {

    @Test
    fun `sanitizeUrl returns original url if it is valid`() {
        val url = "https://www.example.com"
        val sanitized = url.sanitizeUrl()
        assertEquals(url, sanitized)
    }

    @Test
    fun `sanitizeUrl encodes spaces in query params`() {
        val url = "https://magnet:?xt=urn:btih:foo&dn=bar baz"
        val expected = "https://magnet:?xt=urn:btih:foo&dn=bar%20baz"
        val sanitized = url.sanitizeUrl()
        assertEquals(expected, sanitized)
    }

    @Test
    fun `sanitizeUrl encodes spaces in path`() {
        val url = "https://example.com/path with spaces"
        val expected = "https://example.com/path%20with%20spaces"
        val sanitized = url.sanitizeUrl()
        assertEquals(expected, sanitized)
    }

    @Test
    fun `sanitizeUrl returns original url if parsing fails completely`() {
        val url = "not a url"
        val sanitized = url.sanitizeUrl()
        assertEquals(url, sanitized)
    }

    @Test
    fun `sanitizeUrl handles magnet links with spaces`() {
        val url = """
                magnet:?xt=urn:btih:5b3191396fa6a86e1a8a3458a9365b3e75fd872b&dn=RKPrime 25 12 10 Yhivi Sitting On A Parked Cock XXX 480p MP4-XXX [XC]
        """.trimIndent()
        val expected = """
            magnet:?xt=urn:btih:5b3191396fa6a86e1a8a3458a9365b3e75fd872b&dn=RKPrime%2025%2012%2010%20Yhivi%20Sitting%20On%20A%20Parked%20Cock%20XXX%20480p%20MP4-XXX%20[XC]
        """.trimIndent()
        val sanitized = url.sanitizeUrl()
        assertEquals(expected, sanitized)
    }

    @Test
    fun `toOpenableDesktopUri adds https scheme when missing`() {
        val url = "example.com/rss"

        val sanitized = url.toOpenableDesktopUri()

        assertEquals("https://example.com/rss", sanitized)
    }

    @Test
    fun `toOpenableDesktopUri adds https scheme when missing on url with port`() {
        val url = "example.com:8443/rss"

        val sanitized = url.toOpenableDesktopUri()

        assertEquals("https://example.com:8443/rss", sanitized)
    }

    @Test
    fun `toOpenableDesktopUri adds https scheme when missing on localhost url with port`() {
        val url = "localhost:8080/feed"

        val sanitized = url.toOpenableDesktopUri()

        assertEquals("https://localhost:8080/feed", sanitized)
    }

    @Test
    fun `toOpenableDesktopUri trims and encodes url`() {
        val url = "  https://example.com/path with spaces  "

        val sanitized = url.toOpenableDesktopUri()

        assertEquals("https://example.com/path%20with%20spaces", sanitized)
    }

    @Test
    fun `toOpenableDesktopUri keeps magnet links`() {
        val url = "magnet:?xt=urn:btih:foo&dn=bar baz"

        val sanitized = url.toOpenableDesktopUri()

        assertEquals("magnet:?xt=urn:btih:foo&dn=bar%20baz", sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects blank urls`() {
        val sanitized = "   ".toOpenableDesktopUri()

        assertNull(sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects plain text with spaces`() {
        val sanitized = "not a url".toOpenableDesktopUri()

        assertNull(sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects web urls with spaces in authority`() {
        val sanitized = "https://exa mple.com/article".toOpenableDesktopUri()

        assertNull(sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects web urls without authority`() {
        val sanitized = "https://".toOpenableDesktopUri()

        assertNull(sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects unsupported schemes`() {
        val sanitized = "javascript:alert(1)".toOpenableDesktopUri()

        assertNull(sanitized)
    }

    @Test
    fun `toOpenableDesktopUri rejects unsupported schemes with numeric content`() {
        val sanitized = "tel:123".toOpenableDesktopUri()

        assertNull(sanitized)
    }
}
