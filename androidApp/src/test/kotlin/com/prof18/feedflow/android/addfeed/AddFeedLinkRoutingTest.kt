package com.prof18.feedflow.android.addfeed

import androidx.navigation3.runtime.NavKey
import com.prof18.feedflow.android.AddFeed
import com.prof18.feedflow.android.Home
import org.junit.Assert.assertEquals
import org.junit.Test

class AddFeedLinkRoutingTest {

    @Test
    fun `a link opens add feed on top of the current screen`() {
        val backStack = mutableListOf<NavKey>(Home())

        backStack.openAddFeedLink(FEED_URL, requestId = 1)

        assertEquals(listOf(Home(), AddFeed(initialUrl = FEED_URL, requestId = 1)), backStack)
    }

    @Test
    fun `the same link again replaces the open add feed so the field is filled again`() {
        val backStack = mutableListOf<NavKey>(Home(), AddFeed(initialUrl = FEED_URL, requestId = 1))

        backStack.openAddFeedLink(FEED_URL, requestId = 2)

        assertEquals(listOf(Home(), AddFeed(initialUrl = FEED_URL, requestId = 2)), backStack)
    }

    @Test
    fun `a link replaces an add feed opened from the drawer`() {
        val backStack = mutableListOf<NavKey>(Home(), AddFeed())

        backStack.openAddFeedLink(FEED_URL, requestId = 3)

        assertEquals(listOf(Home(), AddFeed(initialUrl = FEED_URL, requestId = 3)), backStack)
    }

    private companion object {
        const val FEED_URL = "https://example.com/feed.xml"
    }
}
