package com.prof18.feedflow.android.widget

import androidx.glance.appwidget.SizeMode
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.shared.data.WidgetSettingsRepository
import com.prof18.feedflow.shared.domain.feed.FeedWidgetRepository
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Test
import sun.misc.Unsafe

class FeedFlowWidgetTest {

    @Test
    fun `widget uses exact size mode`() {
        val widget = FeedFlowWidget(
            repository = instanceWithoutConstructor(FeedWidgetRepository::class.java),
            widgetSettingsRepository = instanceWithoutConstructor(WidgetSettingsRepository::class.java),
            browserManager = instanceWithoutConstructor(BrowserManager::class.java),
        )

        assertEquals(SizeMode.Exact, widget.sizeMode)
    }

    @Test
    fun `limiting feed items preserves the newest-first prefix`() {
        val newestFirstItems = listOf(
            feedItem(id = "newest", pubDateMillis = 3_000L),
            feedItem(id = "middle", pubDateMillis = 2_000L),
            feedItem(id = "oldest", pubDateMillis = 1_000L),
        ).toImmutableList()

        val limitedItems = limitWidgetFeedItems(
            feedItems = newestFirstItems,
            itemCapacity = 2,
        )

        assertEquals(listOf("newest", "middle"), limitedItems.map(FeedItem::id))
    }

    private fun feedItem(
        id: String,
        pubDateMillis: Long,
    ): FeedItem = FeedItem(
        id = id,
        url = "https://example.com/$id",
        title = id,
        subtitle = null,
        content = null,
        imageUrl = null,
        feedSource = feedSource,
        pubDateMillis = pubDateMillis,
        isRead = false,
        dateString = null,
        commentsUrl = null,
        isBookmarked = false,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> instanceWithoutConstructor(type: Class<T>): T =
        unsafe.allocateInstance(type) as T

    private companion object {
        val feedSource = FeedSource(
            id = "source",
            url = "https://example.com/feed",
            title = "Feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )

        val unsafe: Unsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let { field ->
            field.isAccessible = true
            field.get(null) as Unsafe
        }
    }
}
