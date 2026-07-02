package com.prof18.feedflow.shared.presentation

import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.LinkOpeningPreference
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
    fun `fast fling marks all loaded unread items passed by first visible id`() {
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

        assertEquals((1..19).map { FeedItemId("item-$it") }.toSet(), result)
    }

    @Test
    fun `snapshot indices do not decide passed range`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 100),
                visibleItem(id = "item-2", index = 101),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-4", index = 0),
                visibleItem(id = "item-5", index = 1),
            ),
        )

        assertEquals((1..3).map { FeedItemId("item-$it") }.toSet(), result)
    }

    @Test
    fun `unresolved visible ids seed without marking`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "stale-item", index = 0),
            ),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `transient empty snapshot keeps previous boundary`() {
        onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
        )

        val emptyResult = onVisibleItemsChanged(emptyList())

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-4", index = 3),
                visibleItem(id = "item-5", index = 4),
            ),
        )

        assertTrue(emptyResult.isEmpty())
        assertEquals((1..3).map { FeedItemId("item-$it") }.toSet(), result)
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
                visibleItem(id = "item-1", index = 0),
                visibleItem(id = "item-2", index = 1),
            ),
            readIds = setOf("item-1"),
        )

        val result = onVisibleItemsChanged(
            listOf(
                visibleItem(id = "item-3", index = 2),
            ),
            readIds = setOf("item-1"),
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

    @Test
    fun `layout change discards previous visible items`() {
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
            listShapeKey = defaultListShapeKey.copy(feedLayout = FeedLayout.BIG_IMAGE),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `grid mode change discards previous visible items`() {
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
            listShapeKey = defaultListShapeKey.copy(isGridLayoutEnabled = true),
        )

        assertTrue(result.isEmpty())
    }

    private fun onVisibleItemsChanged(
        visibleItems: List<VisibleFeedItem>,
        readIds: Set<String> = emptySet(),
        feedItems: List<FeedItem> = feedItems(readIds = readIds),
        feedListVersion: Long = 0,
        listShapeKey: ScrollReadListShapeKey = defaultListShapeKey,
    ): Set<FeedItemId> =
        tracker.onVisibleItemsChanged(
            visibleItems = visibleItems,
            feedItems = feedItems,
            feedListVersion = feedListVersion,
            listShapeKey = listShapeKey,
        )

    private fun visibleItem(
        id: String,
        index: Int,
    ): VisibleFeedItem =
        VisibleFeedItem(
            id = id,
            index = index,
        )

    private fun feedItems(
        count: Int = 25,
        readIds: Set<String> = emptySet(),
    ): List<FeedItem> =
        (1..count).map { index ->
            val id = "item-$index"
            FeedItem(
                id = id,
                url = "https://example.com/$id",
                title = id,
                subtitle = null,
                content = null,
                imageUrl = null,
                feedSource = feedSource,
                pubDateMillis = index.toLong(),
                isRead = id in readIds,
                dateString = null,
                commentsUrl = null,
                isBookmarked = false,
            )
        }

    private companion object {
        val feedSource = FeedSource(
            id = "source-1",
            url = "https://example.com/feed",
            title = "Source",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            linkOpeningPreference = LinkOpeningPreference.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
        )

        val defaultListShapeKey = ScrollReadListShapeKey(
            showReadArticlesTimeline = false,
            hideReadItems = false,
            feedLayout = FeedLayout.LIST,
            isGridLayoutEnabled = false,
        )
    }
}
