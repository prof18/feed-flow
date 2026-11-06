package com.prof18.feedflow.desktop.home

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefreshScrollPolicyTest {
    @Test
    fun `single pane always scrolls to the top`() {
        assertTrue(shouldScrollToTopOnRefresh(isMultiPane = false, isReaderOpen = false, fromNewArticles = false))
        assertTrue(shouldScrollToTopOnRefresh(isMultiPane = false, isReaderOpen = true, fromNewArticles = false))
    }

    @Test
    fun `multi pane with an empty reader scrolls to the top`() {
        assertTrue(shouldScrollToTopOnRefresh(isMultiPane = true, isReaderOpen = false, fromNewArticles = false))
    }

    @Test
    fun `multi pane with an open article keeps the list position on refresh`() {
        assertFalse(shouldScrollToTopOnRefresh(isMultiPane = true, isReaderOpen = true, fromNewArticles = false))
    }

    @Test
    fun `multi pane scrolls to the top when the new articles pill is clicked`() {
        assertTrue(shouldScrollToTopOnRefresh(isMultiPane = true, isReaderOpen = true, fromNewArticles = true))
    }
}
