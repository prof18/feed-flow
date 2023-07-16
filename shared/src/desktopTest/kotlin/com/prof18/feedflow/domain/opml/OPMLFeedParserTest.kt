@file:Suppress("MagicNumber")

package com.prof18.feedflow.domain.opml

import com.prof18.feedflow.TestDispatcherProvider
import com.prof18.feedflow.opml
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val feedSources = parser.importFeed(opmlInput)
        assertTrue(feedSources.size == 6)
    }

    @Test
    fun `The number of feed in category are correct`() = runTest {
        val feedSources = parser.importFeed(opmlInput)

        val techFeeds = feedSources.filter { it.category == "Tech" }
        val basketFeeds = feedSources.filter { it.category == "Basket" }
        val newsFeeds = feedSources.filter { it.category == "News" }

        assertTrue(techFeeds.size == 3)
        assertTrue(basketFeeds.size == 2)
        assertTrue(newsFeeds.size == 1)
    }

    @Test
    fun `The feeds are parsed correctly`() = runTest {
        val feedSources = parser.importFeed(opmlInput)

        assertEquals("Hacker News", feedSources[0].title)
        assertEquals("https://news.ycombinator.com/rss", feedSources[0].url)
        assertEquals("Tech", feedSources[0].category)

        assertEquals("Android Police - Feed", feedSources[1].title)
        assertEquals("https://www.androidpolice.com/feed/", feedSources[1].url)
        assertEquals("Tech", feedSources[1].category)

        assertEquals("TechCrunch", feedSources[2].title)
        assertEquals("https://techcrunch.com/feed/", feedSources[2].url)
        assertEquals("Tech", feedSources[2].category)

        assertEquals("Pianeta Basket", feedSources[3].title)
        assertEquals("https://www.pianetabasket.com/rss/", feedSources[3].url)
        assertEquals("Basket", feedSources[3].category)

        assertEquals("Overtime", feedSources[4].title)
        assertEquals("https://www.overtimebasket.com/feed/", feedSources[4].url)
        assertEquals("Basket", feedSources[4].category)

        assertEquals("Il Post", feedSources[5].title)
        assertEquals("https://feeds.ilpost.it/ilpost", feedSources[5].url)
        assertEquals("News", feedSources[5].category)
    }
}
