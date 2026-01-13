@file:Suppress("MagicNumber")

package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import com.prof18.feedflow.shared.opml
import com.prof18.feedflow.shared.opmlWithMalformedXml
import com.prof18.feedflow.shared.opmlWithText
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class OPMLFeedParserTest {

    private val parser = OpmlFeedHandler(
        dispatcherProvider = TestDispatcherProvider,
    )

    private val file = File.createTempFile("some-prefix", ".tmp").apply {
        deleteOnExit()
        writeText(opml)
    }

    val opmlInput = OpmlInput(
        file = file,
    )

    @Test
    fun `The number of feeds are correct`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.size == 6)
    }

    @Test
    fun `The number of feed in category are correct`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)

        val techFeeds = feedSources.filter { it.category?.title == "Tech" }
        val basketFeeds = feedSources.filter { it.category?.title == "Basket" }
        val newsFeeds = feedSources.filter { it.category?.title == "News" }

        assertTrue(techFeeds.size == 3)
        assertTrue(basketFeeds.size == 2)
        assertTrue(newsFeeds.size == 1)
    }

    @Test
    fun `The feeds are parsed correctly`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)

        assertEquals("Hacker News", feedSources[0].title)
        assertEquals("https://news.ycombinator.com/rss", feedSources[0].url)
        assertEquals("Tech", feedSources[0].category?.title)

        assertEquals("Android Police - Feed", feedSources[1].title)
        assertEquals("https://www.androidpolice.com/feed/", feedSources[1].url)
        assertEquals("Tech", feedSources[1].category?.title)

        assertEquals("TechCrunch", feedSources[2].title)
        assertEquals("https://techcrunch.com/feed/", feedSources[2].url)
        assertEquals("Tech", feedSources[2].category?.title)

        assertEquals("Pianeta Basket", feedSources[3].title)
        assertEquals("https://www.pianetabasket.com/rss/", feedSources[3].url)
        assertEquals("Basket", feedSources[3].category?.title)

        assertEquals("Overtime", feedSources[4].title)
        assertEquals("https://www.overtimebasket.com/feed/", feedSources[4].url)
        assertEquals("Basket", feedSources[4].category?.title)

        assertEquals("Il Post", feedSources[5].title)
        assertEquals("https://feeds.ilpost.it/ilpost", feedSources[5].url)
        assertEquals("News", feedSources[5].category?.title)
    }

    @Test
    fun `Malformed OPML is parsed correctly`() = runTest {
        val file = File.createTempFile("malformed-", ".tmp").apply {
            deleteOnExit()
            writeText(opmlWithMalformedXml)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertEquals(3, feedSources.size)
        assertEquals("Test & Demo", feedSources[0].title)
        assertEquals("Unclosed tag example", feedSources[1].title)
        assertEquals("Special chars test", feedSources[2].title)
    }

    @Test
    fun `Invalid XML throws OpmlParsingException`() = runTest {
        val file = File.createTempFile("invalid-", ".tmp").apply {
            deleteOnExit()
            writeText("This is not XML at all")
        }

        val opmlInput = OpmlInput(file = file)

        try {
            parser.generateFeedSources(opmlInput)
            fail("Expected OpmlParsingException to be thrown")
        } catch (e: OpmlParsingException) {
            assertTrue(e.message?.contains("Failed to parse OPML file") == true)
        }
    }

    @Test
    fun `The opml with text is parsed correctly`() = runTest {
        val file = File.createTempFile("some-prefix", ".tmp").apply {
            deleteOnExit()
            writeText(opmlWithText)
        }

        val opmlInput = OpmlInput(
            file = file,
        )

        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }

    @Test
    fun `should handle deeply nested categories`() = runTest {
        val opml = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                    <outline text="Level 1">
                        <outline text="Level 2">
                            <outline text="Level 3">
                                <outline text="Deep Feed" xmlUrl="https://example.com/feed.xml" />
                            </outline>
                        </outline>
                    </outline>
                </body>
            </opml>
        """.trimIndent()

        val file = File.createTempFile("nested-", ".tmp").apply {
            deleteOnExit()
            writeText(opml)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertTrue(feedSources.isNotEmpty())
        assertEquals("Deep Feed", feedSources[0].title)
        assertEquals("https://example.com/feed.xml", feedSources[0].url)
    }

    @Test
    fun `should handle duplicate feed URLs`() = runTest {
        val opml = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                    <outline text="Feed 1" xmlUrl="https://example.com/feed.xml" />
                    <outline text="Feed 2" xmlUrl="https://example.com/feed.xml" />
                </body>
            </opml>
        """.trimIndent()

        val file = File.createTempFile("duplicate-", ".tmp").apply {
            deleteOnExit()
            writeText(opml)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertEquals(2, feedSources.size)
    }

    @Test
    fun `should handle BOM in UTF-8 encoding`() = runTest {
        val bomUtf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val opmlContent = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                    <outline text="Feed with BOM" xmlUrl="https://example.com/feed.xml" />
                </body>
            </opml>
        """.trimIndent()

        val file = File.createTempFile("bom-", ".tmp").apply {
            deleteOnExit()
            writeBytes(bomUtf8 + opmlContent.toByteArray())
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertTrue(feedSources.isNotEmpty())
        assertEquals("Feed with BOM", feedSources[0].title)
    }

    @Test
    fun `should handle empty OPML file`() = runTest {
        val opml = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                </body>
            </opml>
        """.trimIndent()

        val file = File.createTempFile("empty-", ".tmp").apply {
            deleteOnExit()
            writeText(opml)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertTrue(feedSources.isEmpty())
    }

    @Test
    fun `should handle OPML with only categories and no feeds`() = runTest {
        val opml = """
            <?xml version="1.0"?>
            <opml version="2.0">
                <body>
                    <outline text="Category 1" />
                    <outline text="Category 2" />
                </body>
            </opml>
        """.trimIndent()

        val file = File.createTempFile("no-feeds-", ".tmp").apply {
            deleteOnExit()
            writeText(opml)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)

        assertTrue(feedSources.isEmpty())
    }
}
