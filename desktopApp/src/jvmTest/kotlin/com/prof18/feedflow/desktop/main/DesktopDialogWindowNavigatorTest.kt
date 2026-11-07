package com.prof18.feedflow.desktop.main

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopDialogWindowNavigatorTest {

    @Test
    fun `opening add feed with a url opens the dialog pre-filled`() {
        val navigator = DesktopDialogWindowNavigator()

        navigator.openAddFeed(FEED_URL)

        assertTrue(navigator.isOpen(DesktopDialogWindowDestination.AddFeed))
        assertEquals(FEED_URL, navigator.addFeedInitialUrl)
    }

    @Test
    fun `closing add feed clears the url`() {
        val navigator = DesktopDialogWindowNavigator()
        navigator.openAddFeed(FEED_URL)

        navigator.close(DesktopDialogWindowDestination.AddFeed)

        assertNull(navigator.addFeedInitialUrl)
    }

    @Test
    fun `a plain open after a close starts empty`() {
        val navigator = DesktopDialogWindowNavigator()
        navigator.openAddFeed(FEED_URL)
        navigator.close(DesktopDialogWindowDestination.AddFeed)

        navigator.open(DesktopDialogWindowDestination.AddFeed)

        assertNull(navigator.addFeedInitialUrl)
    }

    @Test
    fun `the same link opened twice is a new request each time`() {
        val navigator = DesktopDialogWindowNavigator()
        val start = navigator.addFeedRequestRevision

        navigator.openAddFeed(FEED_URL)
        navigator.openAddFeed(FEED_URL)

        assertEquals(start + 2, navigator.addFeedRequestRevision)
    }

    private companion object {
        const val FEED_URL = "https://example.com/feed"
    }
}
