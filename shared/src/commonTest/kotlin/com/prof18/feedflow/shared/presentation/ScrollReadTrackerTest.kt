package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.VisibleFeedItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrollReadTrackerTest {

    private val tracker = ScrollReadTracker()

    @Test
    fun `initial visible snapshot marks nothing`() {
        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `forward scroll marks previously visible unread items that left above`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-2", index = 1),
                visibleItem(id = "item-3", index = 2),
            ),
        )

        assertEquals(setOf(FeedItemId("item-1")), result)
    }

    @Test
    fun `backward scroll marks nothing`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
                visibleItem(id = "item-4", index = 3),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `fast fling marks only observed items`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-20", index = 19),
                visibleItem(id = "item-21", index = 20),
            ),
        )

        assertEquals(setOf(FeedItemId("item-1"), FeedItemId("item-2")), result)
    }

    @Test
    fun `reset discards previous visible items`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        tracker.reset()

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `read items are not marked again`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0, isRead = true),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
        )

        assertEquals(setOf(FeedItemId("item-2")), result)
    }

    @Test
    fun `version change discards previous visible items`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
            feedListVersion = 1,
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
            feedListVersion = 2,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `settings change discards previous visible items`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
            listShapeKey = defaultListShapeKey.copy(hideReadItems = true),
        )

        assertTrue(result.isEmpty())
    }

    private fun onVisibleItemsChanged(
        visibleItems: List<VisibleFeedItem>,
        feedListVersion: Long = 0,
        listShapeKey: ScrollReadListShapeKey = defaultListShapeKey,
    ): Set<FeedItemId> =
        tracker.onVisibleItemsChanged(
            visibleItems = visibleItems,
            feedListVersion = feedListVersion,
            listShapeKey = listShapeKey,
        )

    private fun visibleItem(
        id: String,
        index: Int,
        isRead: Boolean = false,
    ): VisibleFeedItem =
        VisibleFeedItem(
            id = id,
            index = index,
            isRead = isRead,
        )

    private companion object {
        val defaultListShapeKey = ScrollReadListShapeKey(
            showReadArticlesTimeline = false,
            hideReadItems = false,
        )
    }
}
