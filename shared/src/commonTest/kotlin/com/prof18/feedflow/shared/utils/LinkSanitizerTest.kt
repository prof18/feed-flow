package com.prof18.feedflow.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSanitizerTest {

    private val links = listOf(
        "https://www.example.com",
        "www.example.com",
        " www.example.com  ",
    )

    private val correctLink = "https://www.example.com"

    @Test
    fun `sanitizeUrl handles links with www correctly`() {
        for (link in links) {
            val cleanLink = sanitizeUrl(link)
            assertEquals(correctLink, cleanLink)
        }
    }

    @Test
    fun `sanitizeUrl handles links without https and www`() {
        val cleanLink = sanitizeUrl("example.com")
        assertEquals("https://example.com", cleanLink)
    }

    @Test
    fun `sanitizeUrl handles http links correctly`() {
        val cleanLink = sanitizeUrl("http://example.com")
        assertEquals("http://example.com", cleanLink)
    }
}
