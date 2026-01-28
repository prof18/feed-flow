package com.prof18.feedflow.shared

import com.prof18.feedflow.shared.domain.opml.OpmlFeedHandlerAndroid
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.test.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OPMLFeedParserAndroidTest {

    private val parser = OpmlFeedHandlerAndroid(
        dispatcherProvider = TestDispatcherProvider,
    )

    private val opmlInput = OpmlInput(
        inputStream = opml.byteInputStream(),
    )

    @Test
    fun `generateFeedSources parses correct number of feeds`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, actual = 6)
    }

    @Test
    fun `generateFeedSources groups feeds by category correctly`() = runTest {
        val feedSources = parser.generateFeedSources(opmlInput)

        val techFeeds = feedSources.filter { it.category?.title == "Tech" }
        val basketFeeds = feedSources.filter { it.category?.title == "Basket" }
        val newsFeeds = feedSources.filter { it.category?.title == "News" }

        assertEquals(techFeeds.size, actual = 3)
        assertEquals(basketFeeds.size, actual = 2)
        assertEquals(newsFeeds.size, actual = 1)
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
            inputStream = opmlWithText.byteInputStream(),
        )
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }
}
