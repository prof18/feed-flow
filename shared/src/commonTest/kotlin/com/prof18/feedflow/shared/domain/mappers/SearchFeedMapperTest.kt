package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.db.Search
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchFeedMapperTest {

    private val dateFormatter = FakeDateFormatter()

    @Test
    fun `toFeedItem removes title from description when enabled`() {
        val search = createSearch(
            title = "Title",
            subtitle = "Title  Description",
        )

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(removeTitleFromDescription = true),
        )

        assertEquals("Description", result.subtitle)
    }

    @Test
    fun `toFeedItem hides description when requested`() {
        val search = createSearch(subtitle = "Description")

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideDescription = true),
        )

        assertNull(result.subtitle)
    }

    @Test
    fun `toFeedItem hides images when requested`() {
        val search = createSearch(imageUrl = "https://example.com/image.jpg")

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideImages = true),
        )

        assertNull(result.imageUrl)
    }

    @Test
    fun `toFeedItem hides date when requested`() {
        val search = createSearch(pubDate = 1000L)

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideDate = true),
        )

        assertNull(result.dateString)
    }

    @Test
    fun `toFeedItem shows description by default`() {
        val search = createSearch(subtitle = "Description")

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(),
        )

        assertEquals("Description", result.subtitle)
    }

    @Test
    fun `toFeedItem shows image by default`() {
        val search = createSearch(imageUrl = "https://example.com/image.jpg")

        val result = search.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(),
        )

        assertEquals("https://example.com/image.jpg", result.imageUrl)
    }

    private fun createSearch(
        title: String = "Title",
        subtitle: String? = "Subtitle",
        imageUrl: String? = null,
        pubDate: Long? = 1000L,
    ): Search = Search(
        url_hash = "item-1",
        url = "https://example.com/item-1",
        title = title,
        subtitle = subtitle,
        content = null,
        image_url = imageUrl,
        feed_source_id = "source-1",
        is_read = false,
        is_bookmarked = false,
        pub_date = pubDate,
        comments_url = "https://example.com/comments",
        notification_sent = false,
        is_blocked = false,
        content_fetched = false,
        feed_source_title = "Feed Source",
        feed_source_id_ = "source-1",
        feed_source_url = "https://example.com/feed.xml",
        feed_source_last_sync_timestamp = null,
        feed_source_fetch_failed = false,
        feed_source_category_id = null,
        feed_source_category_title = null,
        feed_source_logo_url = "https://example.com/logo.png",
        feed_source_link_opening_preference = null,
    )

    private class FakeDateFormatter : DateFormatter {
        override fun getDateMillisFromString(dateString: String): Long? = null
        override fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String =
            "formatted-$millis"
        override fun formatDateForLastRefresh(millis: Long): String = "formatted-refresh"
        override fun currentTimeMillis(): Long = 0
        override fun getCurrentDateForExport(): String = "2023-11-14"
    }
}
