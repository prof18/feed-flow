package com.prof18.feedflow.android.addfeed

import com.prof18.feedflow.core.utils.AddFeedLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SharedFeedRequestTest {

    @Test
    fun `the url comes from the shared text`() {
        assertEquals("https://example.com/feed", sharedFeedUrl(text = "Look https://example.com/feed", html = null))
    }

    @Test
    fun `the html is used when the text has no link`() {
        assertEquals(
            "https://example.com/rss",
            sharedFeedUrl(text = "no link", html = "<a href=\"https://example.com/rss\">"),
        )
    }

    @Test
    fun `a share without a link has no url`() {
        assertNull(sharedFeedUrl(text = null, html = null))
        assertNull(sharedFeedUrl(text = "hello", html = "<b>hi</b>"))
    }

    @Test
    fun `the open in feedflow link round trips through the add feed parser`() {
        val link = openInFeedFlowLink("https://example.com/rss?a=1&b=2")

        assertNotNull(link)
        assertEquals("https://example.com/rss?a=1&b=2", AddFeedLinkParser.parse(requireNotNull(link)))
    }

    @Test
    fun `no open in feedflow link for a url the app would reject`() {
        assertNull(openInFeedFlowLink("https://exa%zzmple.com/feed"))
    }
}
