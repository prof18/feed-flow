package com.prof18.feedflow.shared.utils

import com.prof18.feedflow.shared.test.FakeClock
import kotlin.test.Test
import kotlin.test.assertEquals

class ArchiveISTest {

    private val fakeClock = FakeClock.DEFAULT
    private val expectedYear = 2025

    @Test
    fun `should generate archive URL with correct format`() {
        val originalUrl = "https://example.com/article/123"
        val archiveUrl = getArchiveISUrl(originalUrl, fakeClock)

        assertEquals("https://archive.is/$expectedYear/$originalUrl", archiveUrl)
    }

    @Test
    fun `should handle empty URL`() {
        val result = getArchiveISUrl("", fakeClock)

        assertEquals("https://archive.is/$expectedYear/", result)
    }

    @Test
    fun `should preserve complete URL structure`() {
        val originalUrl = "https://blog.example.com:8080/path/to/article?param=value&other=123#anchor"
        val archiveUrl = getArchiveISUrl(originalUrl, fakeClock)

        assertEquals("https://archive.is/$expectedYear/$originalUrl", archiveUrl)
    }
}
