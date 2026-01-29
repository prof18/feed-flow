package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpmlBOMParsingIosTest {

    private val parser = OpmlFeedHandlerIos(
        dispatcherProvider = TestDispatcherProvider,
    )

    private val opmlContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="1.0">
        <head>
        <title>Subscriptions from FeedFlow</title>
        </head>
        <body>
        <outline text="Tech" title="Tech">
        <outline type="rss" text="Hacker News" title="Hacker News" xmlUrl="https://news.ycombinator.com/rss" htmlUrl="https://news.ycombinator.com/rss"/>
        </outline>
        </body>
        </opml>
    """.trimIndent()

    @Test
    fun `generateFeedSources parses OPML with BOM correctly`() = runTest {
        val contentWithBom = "\uFEFF$opmlContent"

        val data = NSString.create(string = contentWithBom).dataUsingEncoding(NSUTF8StringEncoding)

        val opmlInput = OpmlInput(opmlData = data!!)

        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }

    @Test
    fun `generateFeedSources parses OPML with ampersand correctly`() = runTest {
        val opmlWithAmpersand = """
            <?xml version="1.0" encoding="UTF-8"?>
            <opml version="1.0">
            <head><title>Subs</title></head>
            <body>
            <outline text="R & D" title="R & D" xmlUrl="http://example.com/rss?a=1&b=2" />
            <outline text="Unknown &unknown;" title="Unknown &unknown;" xmlUrl="http://example.com/rss" />
            </body>
            </opml>
        """.trimIndent()

        val data = NSString.create(string = opmlWithAmpersand).dataUsingEncoding(NSUTF8StringEncoding)

        val opmlInput = OpmlInput(opmlData = data!!)

        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, 2)
    }
}
