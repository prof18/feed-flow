package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.db.SelectFeeds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            settings = FeedItemMappingSettings(removeTitleFromDescription = true),
        )

        assertEquals("Description", result.subtitle)
    }

    @Test
    fun `toFeedItem hides description when requested`() {
        val selectFeeds = createSelectFeeds(subtitle = "Description")

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideDescription = true),
        )

        assertNull(result.subtitle)
    }

    @Test
    fun `toFeedItem hides images when requested`() {
        val selectFeeds = createSelectFeeds(imageUrl = "https://example.com/image.jpg")

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideImages = true),
        )

        assertNull(result.imageUrl)
    }

    @Test
    fun `toFeedItem hides images when the feed source opts out`() {
        val selectFeeds = createSelectFeeds(
            imageUrl = "https://example.com/image.jpg",
            feedSourceHideImages = true,
        )

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideImages = false),
        )

        assertNull(result.imageUrl)
        assertTrue(result.feedSource.isHideImagesEnabled)
    }

    @Test
    fun `toFeedItem keeps images when the feed source does not opt out`() {
        val selectFeeds = createSelectFeeds(
            imageUrl = "https://example.com/image.jpg",
            feedSourceHideImages = false,
        )

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideImages = false),
        )

        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertFalse(result.feedSource.isHideImagesEnabled)
    }

    @Test
    fun `toFeedItem hides date when requested`() {
        val selectFeeds = createSelectFeeds(pubDate = 1000L)

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(hideDate = true),
        )

        assertNull(result.dateString)
    }

    @Test
    fun `toFeedItem uses defaults for missing feed source preferences`() {
        val selectFeeds = createSelectFeeds(
            feedSourceCategoryId = "cat-1",
            feedSourceCategoryTitle = "Tech",
            articleOpenMode = null,
            isHidden = null,
            isPinned = null,
            isNotificationEnabled = null,
            feedSourceHideImages = null,
        )

        val result = selectFeeds.toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(),
        )

        assertEquals(ArticleOpenMode.DEFAULT, result.feedSource.articleOpenMode)
        assertEquals(ArticleOpenMode.DEFAULT, result.feedSource.articleOpenMode)
        assertFalse(result.feedSource.isHiddenFromTimeline)
        assertFalse(result.feedSource.isPinned)
        assertFalse(result.feedSource.isNotificationEnabled)
        assertFalse(result.feedSource.isHideImagesEnabled)
        assertEquals(FeedSourceCategory("cat-1", "Tech"), result.feedSource.category)
    }

    @Test
    fun `toFeedItem maps per-feed content source`() {
        val result = createSelectFeeds(articleOpenMode = ArticleOpenMode.FEED_CONTENT).toFeedItem(
            dateFormatter = dateFormatter,
            settings = FeedItemMappingSettings(),
        )

        assertEquals(ArticleOpenMode.FEED_CONTENT, result.feedSource.articleOpenMode)
    }

    private fun createSelectFeeds(
        title: String = "Title",
        subtitle: String? = "Subtitle",
        imageUrl: String? = null,
        pubDate: Long? = 1000L,
        feedSourceCategoryId: String? = null,
        feedSourceCategoryTitle: String? = null,
        articleOpenMode: ArticleOpenMode? = ArticleOpenMode.INTERNAL_BROWSER,
        isHidden: Boolean? = false,
        isPinned: Boolean? = false,
        isNotificationEnabled: Boolean? = false,
        feedSourceHideImages: Boolean? = false,
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
        feed_source_article_open_mode = articleOpenMode,
        feed_source_is_hidden = isHidden,
        feed_source_is_pinned = isPinned,
        feed_source_notifications_enabled = isNotificationEnabled,
        feed_source_hide_images = feedSourceHideImages,
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
