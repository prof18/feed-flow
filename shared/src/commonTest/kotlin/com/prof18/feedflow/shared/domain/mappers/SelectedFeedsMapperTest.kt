package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.db.SelectFeeds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SelectedFeedsMapperTest {

    private val dateFormatter = FakeDateFormatter()

    @Test
    fun `toFeedItem removes title from description when enabled`() {
        val selectFeeds = createSelectFeeds(
            title = "Title",
            subtitle = "Title  Description",
        )

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            removeTitleFromDesc = true,
            hideDescription = false,
            hideImages = false,
            hideDate = false,
            dateFormat = DateFormat.NORMAL,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertEquals("Description", result.subtitle)
    }

    @Test
    fun `toFeedItem hides description when requested`() {
        val selectFeeds = createSelectFeeds(subtitle = "Description")

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            removeTitleFromDesc = false,
            hideDescription = true,
            hideImages = false,
            hideDate = false,
            dateFormat = DateFormat.NORMAL,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertNull(result.subtitle)
    }

    @Test
    fun `toFeedItem hides images when requested`() {
        val selectFeeds = createSelectFeeds(imageUrl = "https://example.com/image.jpg")

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            removeTitleFromDesc = false,
            hideDescription = false,
            hideImages = true,
            hideDate = false,
            dateFormat = DateFormat.NORMAL,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertNull(result.imageUrl)
    }

    @Test
    fun `toFeedItem hides date when requested`() {
        val selectFeeds = createSelectFeeds(pubDate = 1000L)

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            removeTitleFromDesc = false,
            hideDescription = false,
            hideImages = false,
            hideDate = true,
            dateFormat = DateFormat.NORMAL,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertNull(result.dateString)
    }

    @Test
    fun `toFeedItem uses defaults for missing feed source preferences`() {
        val selectFeeds = createSelectFeeds(
            feedSourceCategoryId = "cat-1",
            feedSourceCategoryTitle = "Tech",
            linkOpeningPreference = null,
            isHidden = null,
            isPinned = null,
            isNotificationEnabled = null,
        )

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            removeTitleFromDesc = false,
            hideDescription = false,
            hideImages = false,
            hideDate = false,
            dateFormat = DateFormat.NORMAL,
            timeFormat = TimeFormat.HOURS_24,
        )

        assertEquals(LinkOpeningPreference.DEFAULT, result.feedSource.linkOpeningPreference)
        assertFalse(result.feedSource.isHiddenFromTimeline)
        assertFalse(result.feedSource.isPinned)
        assertFalse(result.feedSource.isNotificationEnabled)
        assertEquals(FeedSourceCategory("cat-1", "Tech"), result.feedSource.category)
    }

    private fun createSelectFeeds(
        title: String = "Title",
        subtitle: String? = "Subtitle",
        imageUrl: String? = null,
        pubDate: Long? = 1000L,
        feedSourceCategoryId: String? = null,
        feedSourceCategoryTitle: String? = null,
        linkOpeningPreference: LinkOpeningPreference? = LinkOpeningPreference.INTERNAL_BROWSER,
        isHidden: Boolean? = false,
        isPinned: Boolean? = false,
        isNotificationEnabled: Boolean? = false,
    ): SelectFeeds = SelectFeeds(
        url_hash = "item-1",
        url = "https://example.com/item-1",
        title = title,
        subtitle = subtitle,
        image_url = imageUrl,
        pub_date = pubDate,
        comments_url = "https://example.com/comments",
        is_read = false,
        is_bookmarked = false,
        notification_sent = false,
        feed_source_title = "Feed Source",
        feed_source_id = "source-1",
        feed_source_url = "https://example.com/feed.xml",
        feed_source_last_sync_timestamp = null,
        feed_source_category_id = feedSourceCategoryId,
        feed_source_category_title = feedSourceCategoryTitle,
        feed_source_logo_url = "https://example.com/logo.png",
        feed_source_link_opening_preference = linkOpeningPreference,
        feed_source_is_hidden = isHidden,
        feed_source_is_pinned = isPinned,
        feed_source_notifications_enabled = isNotificationEnabled,
        feed_source_fetch_failed = false,
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
