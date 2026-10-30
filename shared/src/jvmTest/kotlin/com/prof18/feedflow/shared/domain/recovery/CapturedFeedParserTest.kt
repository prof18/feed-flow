package com.prof18.feedflow.shared.domain.recovery

import com.prof18.rssparser.RssParserBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CapturedFeedParserTest {

    private val parser = CapturedFeedParser(RssParserBuilder().build()::parse)

    @Test
    fun `utf8 rss bytes parse to a channel`() = runTest {
        val bytes = rss(title = "Verified feed").encodeToByteArray()

        val channel = parser.parse(bytes, contentType = "application/rss+xml")

        assertEquals("Verified feed", channel.title)
        assertEquals(2, channel.items.size)
    }

    @Test
    fun `iso 8859 1 bytes keep accented characters`() = runTest {
        val bytes = rss(title = "Café").toByteArray(Charsets.ISO_8859_1)

        val channel = parser.parse(bytes, contentType = "text/xml; charset=ISO-8859-1")

        assertEquals("Café", channel.title)
    }

    @Test
    fun `relative item links are kept`() = runTest {
        val channel = parser.parse(rss(title = "Relative").encodeToByteArray(), contentType = null)

        assertEquals("/post/1", channel.items.first().link)
    }

    private fun rss(title: String) = """
        <?xml version="1.0"?>
        <rss version="2.0">
          <channel>
            <title>$title</title>
            <link>https://e.test</link>
            <description>Fixture</description>
            <item><title>One</title><link>/post/1</link></item>
            <item><title>Two</title><link>https://e.test/post/2</link></item>
          </channel>
        </rss>
    """.trimIndent()
}
