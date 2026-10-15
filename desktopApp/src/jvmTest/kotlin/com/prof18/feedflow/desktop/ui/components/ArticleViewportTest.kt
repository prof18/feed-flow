package com.prof18.feedflow.desktop.ui.components

import com.prof18.feedflow.core.model.VisibleFeedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArticleViewportTest {
    @Test
    fun `footer cannot be mistaken for an article with the same id`() {
        val ids = listOf("footer", "second")
        assertEquals(
            listOf(VisibleFeedItem("footer", 0)),
            visibleArticles(ids, listOf(ArticleKey("footer"), ArticleFooterKey)),
        )
    }

    @Test
    fun `visibility follows ids after read items leave the timeline`() {
        assertEquals(
            listOf(VisibleFeedItem("third", 1), VisibleFeedItem("second", 0)),
            visibleArticles(
                listOf("second", "third"),
                listOf(ArticleKey("third"), ArticleKey("first"), ArticleKey("second")),
            ),
        )
    }

    @Test
    fun `empty or unmeasured viewport does not request a page`() {
        assertNull(articlePageRequest(emptyList(), emptyList()))
        assertNull(articlePageRequest(listOf("one"), emptyList()))
        assertNull(articlePageRequest(listOf("one"), listOf(ArticleFooterKey)))
    }

    @Test
    fun `request near end is keyed to the loaded tail so next page can load without scrolling`() {
        val ids = (0..29).map(Int::toString)
        assertNull(articlePageRequest(ids, listOf(ArticleKey("4"))))
        assertEquals("29", articlePageRequest(ids, listOf(ArticleKey("16"))))
        assertEquals("39", articlePageRequest(ids + (30..39).map(Int::toString), listOf(ArticleKey("29"))))
    }

    @Test
    fun `arrow focus survives a prepend and repairs removal without activation`() {
        assertEquals("second", moveArticleFocus(listOf("first", "second", "third"), "first", 1))
        assertEquals("third", moveArticleFocus(listOf("first", "second", "third"), "third", 1))
        assertEquals("first", moveArticleFocus(listOf("first", "second"), "removed", 1))
        assertNull(moveArticleFocus(emptyList(), "removed", 1))
    }
}
