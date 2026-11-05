package com.prof18.feedflow.shared.presentation.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleRevealTest {

    @Test
    fun `items inside the content area are fully visible`() {
        assertTrue(isVisible(itemStart = 0, itemSize = 100, afterContentPadding = 0))
        assertTrue(isVisible(itemStart = 400, itemSize = 100, afterContentPadding = 0))
    }

    @Test
    fun `items cut by an edge or hidden by the bottom padding are not fully visible`() {
        assertFalse(isVisible(itemStart = -1, itemSize = 100, afterContentPadding = 0))
        assertFalse(isVisible(itemStart = 401, itemSize = 100, afterContentPadding = 0))
        assertFalse(isVisible(itemStart = 350, itemSize = 100, afterContentPadding = 100))
    }

    private fun isVisible(itemStart: Int, itemSize: Int, afterContentPadding: Int): Boolean =
        isFullyVisibleInViewport(
            itemStart = itemStart,
            itemSize = itemSize,
            viewportEndOffset = VIEWPORT_END_OFFSET,
            afterContentPadding = afterContentPadding,
        )

    private companion object {
        const val VIEWPORT_END_OFFSET = 500
    }
}
