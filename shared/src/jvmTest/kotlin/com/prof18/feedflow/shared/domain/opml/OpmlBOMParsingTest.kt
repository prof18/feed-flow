package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Validates that OPML files beginning with a Byte Order Mark (BOM) are parsed correctly.
 */
class OpmlBOMParsingTest {

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
    fun `OPML with BOM should parse correctly`() = runTest {
        val file = File.createTempFile("bom-", ".tmp").apply {
            deleteOnExit()
            // Add BOM \uFEFF
            writeText("\uFEFF$opmlContent")
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }
}
