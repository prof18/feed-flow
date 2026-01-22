package com.prof18.feedflow.shared.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UrlUtilsTest {

    @Test
    fun `isValidUrl returns true for valid URLs`() {
        val validUrls = listOf(
            "https://example.com",
            "http://example.com",
            "https://example.com/feed.xml",
            "https://blog.example.com/rss",
            "https://example.com:8080/feed",
        )
        validUrls.forEach { url ->
            assertTrue(isValidUrl(url), "Expected $url to be valid")
        }
    }

    @Test
    fun `isValidUrl returns false for invalid URLs`() {
        val invalidUrls = listOf(
            "",
            "   ",
            "https://",
            "http://",
            "https:// ",
            "http:// ",
        )
        invalidUrls.forEach { url ->
            assertFalse(isValidUrl(url), "Expected $url to be invalid")
        }
    }

    @Test
    fun `isValidUrl handles URLs with whitespace`() {
        assertTrue(isValidUrl("  https://example.com  "))
    }

    @Test
    fun `isValidUrl rejects protocol only URLs`() {
        assertFalse(isValidUrl("https://"))
        assertFalse(isValidUrl("http://"))
    }
}
