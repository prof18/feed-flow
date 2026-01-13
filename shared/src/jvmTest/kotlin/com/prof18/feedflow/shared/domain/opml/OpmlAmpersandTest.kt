package com.prof18.feedflow.shared.domain.opml

import com.prof18.feedflow.shared.test.TestDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpmlAmpersandTest {

    private val parser = OpmlFeedHandler(
        dispatcherProvider = TestDispatcherProvider,
    )

    // Scenario 1: Ampersand in attribute value
    private val opmlWithAmpersandInAttribute = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="1.0">
        <head>
        <title>Subscriptions</title>
        </head>
        <body>
        <outline text="R & D" title="R & D">
        <outline type="rss" text="Feed & Flow" title="Feed & Flow" xmlUrl="https://example.com/rss?category=a&b" htmlUrl="https://example.com/rss?category=a&b"/>
        </outline>
        </body>
        </opml>
    """.trimIndent()

    // Scenario 2: Ampersand followed by text that looks like an entity but isn't
    private val opmlWithTrickyAmpersand = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="1.0">
        <head><title>Subs</title></head>
        <body>
        <outline text="Tom & Jerry" title="Tom & Jerry" xmlUrl="http://example.com/1" />
        <outline text="AT&T" title="AT&T" xmlUrl="http://example.com/2" />
        <outline text="Q&A" title="Q&A" xmlUrl="http://example.com/3" />
        </body>
        </opml>
    """.trimIndent()

    // Scenario 3: Ampersand at EOF or followed by weird chars
    private val opmlEdgeCases = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="1.0">
        <head><title>Subs</title></head>
        <body>
        <outline text="End&" title="End&" xmlUrl="http://example.com/4" />
        <outline text="Space& " title="Space& " xmlUrl="http://example.com/5" />
        <outline text="Hash&#" title="Hash&#" xmlUrl="http://example.com/6" />
        </body>
        </opml>
    """.trimIndent()

    @Test
    fun `OPML with ampersand in attributes should parse correctly`() = runTest {
        val file = File.createTempFile("ampersand-1-", ".tmp").apply {
            deleteOnExit()
            writeText(opmlWithAmpersandInAttribute)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }

    @Test
    fun `OPML with tricky ampersands should parse correctly`() = runTest {
        val file = File.createTempFile("ampersand-2-", ".tmp").apply {
            deleteOnExit()
            writeText(opmlWithTrickyAmpersand)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, 3)
    }

    @Test
    fun `OPML with edge case should parse correctly`() = runTest {
        val file = File.createTempFile("ampersand-3-", ".tmp").apply {
            deleteOnExit()
            writeText(opmlEdgeCases)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)
        assertEquals(feedSources.size, 3)
    }

    // Scenario 4: Unknown entity
    private val opmlWithUnknownEntity = """
        <?xml version="1.0" encoding="UTF-8"?>
        <opml version="1.0">
        <head><title>Subs</title></head>
        <body>
        <outline text="Unknown &unknown;" title="Unknown &unknown;" xmlUrl="http://example.com/7" />
        </body>
        </opml>
    """.trimIndent()

    @Test
    fun `OPML with unknown entity should parse correctly`() = runTest {
        val file = File.createTempFile("ampersand-4-", ".tmp").apply {
            deleteOnExit()
            writeText(opmlWithUnknownEntity)
        }

        val opmlInput = OpmlInput(file = file)
        val feedSources = parser.generateFeedSources(opmlInput)
        assertTrue(feedSources.isNotEmpty())
    }
}
