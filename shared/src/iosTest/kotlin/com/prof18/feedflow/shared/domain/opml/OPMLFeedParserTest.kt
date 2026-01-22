package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.opml
import com.prof18.feedflow.shared.opmlWithText
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("CAST_NEVER_SUCCEEDS")
class OPMLFeedParserTest {

    private val parser = OpmlFeedHandler(
        dispatcherProvider = TestDispatcherProvider,
    )

    private val opmlInput = OpmlInput(
        opmlData = (opml as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: NSData(),
    )

    @Test
    fun `generateFeedSources parses correct number of feeds`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, 6)
    }

    @Test
    fun `generateFeedSources groups feeds by category correctly`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)

        val techFeeds = feedSources.filter { it.category?.title == "Tech" }
        val basketFeeds = feedSources.filter { it.category?.title == "Basket" }
        val newsFeeds = feedSources.filter { it.category?.title == "News" }

        assertEquals(techFeeds.size, 3)
        assertEquals(basketFeeds.size, 2)
        assertEquals(newsFeeds.size, 1)
    }

    @Test
    fun `generateFeedSources extracts feed details correctly`() = runTest {
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
    fun `generateFeedSources parses OPML with text attribute`() = runTest {
        val opmlInput = OpmlInput(
            opmlData = (opmlWithText as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: NSData(),
        )
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }
}
