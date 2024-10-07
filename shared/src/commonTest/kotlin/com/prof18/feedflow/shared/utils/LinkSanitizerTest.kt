package com.prof18.feedflow.shared.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSanitizerTest {

    private val links = listOf(
        "https://www.example.com",
        "http://www.example.com",
        "www.example.com",
        " www.example.com  ",
    )

    private val correctLink = "https://www.example.com"

    @Test
    fun `The sanitizeUrl works correctly with links with www`() {
        for (link in links) {
            val cleanLink = sanitizeUrl(link)
            assertEquals(correctLink, cleanLink)
        }
    }

    @Test
    fun `The sanitize url works correctly with no https and www links`() {
        val cleanLink = sanitizeUrl("example.com")
        assertEquals("https://example.com", cleanLink)
    }
}
