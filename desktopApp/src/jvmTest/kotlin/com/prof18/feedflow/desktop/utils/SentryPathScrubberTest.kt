package com.prof18.feedflow.desktop.utils

import io.sentry.SentryEvent
import io.sentry.protocol.Message
import io.sentry.protocol.SentryException
import kotlin.test.Test
import kotlin.test.assertEquals

class SentryPathScrubberTest {

    private val home = "/Users/someone"

    @Test
    fun `home paths are replaced in exception values and messages`() {
        val event = SentryEvent().apply {
            message = Message().apply {
                formatted = "Could not open /Users/someone/Library/FeedFlow/FeedFlow.db"
                message = "Could not open /Users/someone/Library/FeedFlow/FeedFlow.db"
            }
            exceptions = listOf(
                SentryException().apply { value = "No native library in paths=[/Users/someone/lib, /usr/lib]" },
                SentryException().apply { value = null },
            )
        }

        scrubHomePaths(event, home)

        assertEquals("Could not open ~/Library/FeedFlow/FeedFlow.db", event.message?.formatted)
        assertEquals("Could not open ~/Library/FeedFlow/FeedFlow.db", event.message?.message)
        assertEquals("No native library in paths=[~/lib, /usr/lib]", event.exceptions?.first()?.value)
        assertEquals(null, event.exceptions?.last()?.value)
    }

    @Test
    fun `a missing home leaves the event unchanged`() {
        val event = SentryEvent().apply {
            exceptions = listOf(SentryException().apply { value = "/Users/someone/x" })
        }

        scrubHomePaths(event, null)

        assertEquals("/Users/someone/x", event.exceptions?.single()?.value)
    }
}
