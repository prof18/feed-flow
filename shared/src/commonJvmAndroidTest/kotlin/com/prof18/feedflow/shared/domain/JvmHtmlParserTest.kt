package com.prof18.feedflow.shared.domain

import co.touchlab.kermit.Logger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JvmHtmlParserTest {

    private val parser = JvmHtmlParser(Logger.withTag("JvmHtmlParserTest"))

    @Test
    fun `When a text has HTML tags then getTextFromHTML returns the text without HTML tags`() {
        val html = """
                <div class="feat-image"><img src="https://9to5mac.com/wp-content/uploads/sites/6/2022/06/get-macos-ventura.jpg?quality=82&#038;strip=all&#038;w=1280" /></div> <p>Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple.</p> <p> <a href="https://9to5mac.com/2023/02/13/macos-ventura-13-2-1-update/#more-864283" class="more-link">more…</a></p> <p>The post <a rel="nofollow" href="https://9to5mac.com/2023/02/13/macos-ventura-13-2-1-update/">Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users</a> appeared first on <a rel="nofollow" href="https://9to5mac.com">9to5Mac</a>.</p>
        """.trimIndent()

        val text = parser.getTextFromHTML(html)

        val expectedText = """
            Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple. more… The post Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users appeared first on 9to5Mac.
        """.trimIndent()

        assertEquals(expectedText, text)
    }

    @Test
    fun `When text is not HTML then getTextFromHTML returns the text`() {
        val text = """
            Apple on Monday released macOS Ventura 13.2.1 for Mac users. According to Apple, the update brings “important bug fixes,” but there are no details on what exactly today’s update fixes. The update comes three weeks after the release of macOS 13.2, which introduced support for Security Keys with Apple. more… The post Apple releases macOS Ventura 13.2.1 with important bug fixes for Mac users appeared first on 9to5Mac.
        """.trimIndent()

        val cleanText = parser.getTextFromHTML(text)

        assertEquals(text, cleanText)
    }

    @Test
    fun `should find RSS feed link`() {
        val html = """
            <html>
                <head>
                    <link rel="alternate" type="application/rss+xml" href="/feed.xml" />
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.getRssUrl(html)
        assertEquals("/feed.xml", feedUrl)
    }

    @Test
    fun `should find Atom feed link`() {
        val html = """
            <html>
                <head>
                    <link rel="alternate" type="application/atom+xml" href="/atom.xml" />
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.getRssUrl(html)
        assertEquals("/atom.xml", feedUrl)
    }

    @Test
    fun `should find JSON feed link`() {
        val html = """
            <html>
                <head>
                    <link rel="alternate" type="application/feed+json" href="/feed.json" />
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.getRssUrl(html)
        assertEquals("/feed.json", feedUrl)
    }

    @Test
    fun `should extract favicon URL`() {
        val html = """
            <html>
                <head>
                    <link rel="icon" href="/favicon.ico" />
                </head>
            </html>
        """.trimIndent()

        val faviconUrl = parser.getFaviconUrl(html)
        assertEquals("/favicon.ico", faviconUrl)
    }

    @Test
    fun `should extract shortcut icon URL`() {
        val html = """
            <html>
                <head>
                    <link rel="shortcut" href="/shortcut-icon.png" />
                </head>
            </html>
        """.trimIndent()

        val faviconUrl = parser.getFaviconUrl(html)
        assertEquals("/shortcut-icon.png", faviconUrl)
    }

    @Test
    fun `should handle malformed HTML`() {
        val html = "<html><p>Unclosed paragraph<div>Unclosed div"

        val text = parser.getTextFromHTML(html)
        assertNotNull(text)
    }

    @Test
    fun `should return null when no feed URL found`() {
        val html = """
            <html>
                <head>
                    <title>No feeds here</title>
                </head>
            </html>
        """.trimIndent()

        val feedUrl = parser.getRssUrl(html)
        assertNull(feedUrl)
    }

    @Test
    fun `should return null when no favicon found`() {
        val html = """
            <html>
                <head>
                    <title>No icon here</title>
                </head>
            </html>
        """.trimIndent()

        val faviconUrl = parser.getFaviconUrl(html)
        assertNull(faviconUrl)
    }

    @Test
    fun `should handle empty HTML`() {
        val text = parser.getTextFromHTML("")
        assertEquals("", text)
    }
}
