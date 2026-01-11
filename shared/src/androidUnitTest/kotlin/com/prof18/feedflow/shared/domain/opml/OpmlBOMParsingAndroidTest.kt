package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OpmlBOMParsingAndroidTest {

    private val parser = OpmlFeedHandler(
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
    fun `Android OPML with BOM should parse correctly`() = runTest {
        // Create input stream with BOM \uFEFF
        val contentWithBom = "\uFEFF$opmlContent"
        val inputStream = ByteArrayInputStream(contentWithBom.toByteArray(Charsets.UTF_8))

        val opmlInput = OpmlInput(inputStream = inputStream)

        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }

    @Test
    fun `Android OPML with ampersand should parse correctly`() = runTest {
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

        val inputStream = ByteArrayInputStream(opmlWithAmpersand.toByteArray(Charsets.UTF_8))
        val opmlInput = OpmlInput(inputStream = inputStream)

        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, 2)
    }
}
