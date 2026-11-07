package com.prof18.feedflow.desktop.startup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AddFeedLinkInboxTest {

    @Test
    fun `a new inbox holds no link`() {
        assertNull(AddFeedLinkInbox().links.value)
    }

    @Test
    fun `an offered link is held`() {
        val inbox = AddFeedLinkInbox()

        inbox.offer(FIRST)

        assertEquals(FIRST, inbox.links.value)
    }

    @Test
    fun `a second offer replaces the first`() {
        val inbox = AddFeedLinkInbox()

        inbox.offer(FIRST)
        inbox.offer(SECOND)

        assertEquals(SECOND, inbox.links.value)
    }

    @Test
    fun `consuming the held link clears it`() {
        val inbox = AddFeedLinkInbox()
        inbox.offer(FIRST)

        inbox.consume(FIRST)

        assertNull(inbox.links.value)
    }

    @Test
    fun `consuming an older link keeps the newer one`() {
        val inbox = AddFeedLinkInbox()
        inbox.offer(FIRST)
        inbox.offer(SECOND)

        inbox.consume(FIRST)

        assertEquals(SECOND, inbox.links.value)
    }

    private companion object {
        const val FIRST = "https://example.com/feed"
        const val SECOND = "https://example.com/other"
    }
}
