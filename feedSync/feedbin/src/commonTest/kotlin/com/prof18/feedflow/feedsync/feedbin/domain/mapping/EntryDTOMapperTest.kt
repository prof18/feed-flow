package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class EntryDTOMapperTest {

    private val htmlParser = FakeHtmlParser()
    private val dateFormatter = FakeDateFormatter()
    private val mapper = EntryDTOMapper(htmlParser, dateFormatter)

    private val feedSource = FeedSource(
        id = "source-1",
        url = "https://example.com/feed.xml",
        title = "Example",
        category = FeedSourceCategory(id = "cat-1", title = "Tech"),
        lastSyncTimestamp = null,
        logoUrl = null,
        websiteUrl = "https://example.com",
        fetchFailed = false,
        linkOpeningPreference = LinkOpeningPreference.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
    )

    @Test
    fun `mapToFeedItem maps fields and formats date when published is valid`() {
        val published = "2023-11-14T10:00:00Z"
        val entryDTO = createEntryDTO(
            id = 42,
            published = published,
            summary = "<p>Summary</p>",
            content = "<p>Content</p> https://example.com/image.jpg",
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = true,
            isBookmarked = false,
        )

        assertEquals("42", result.id)
        assertEquals("https://example.com/article", result.url)
        assertEquals("Parsed Summary", result.subtitle)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertEquals(true, result.isRead)
        assertEquals(false, result.isBookmarked)
        assertEquals("formatted-date", result.dateString)
        assertEquals(Instant.parse(published).toEpochMilliseconds() / 1000, dateFormatter.lastMillis)
    }

    @Test
    fun `mapToFeedItem returns null date when published is invalid`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "not-a-date",
            summary = "Summary",
            content = null,
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertNull(result.pubDateMillis)
        assertNull(result.dateString)
    }

    @Test
    fun `mapToFeedItem extracts image from summary when content is null`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = "Summary https://example.com/summary.png",
            content = null,
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertEquals("https://example.com/summary.png", result.imageUrl)
    }

    @Test
    fun `mapToFeedItem filters emoji images`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = "Summary https://s.w.org/images/core/emoji/test.png",
            content = null,
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertNull(result.imageUrl)
    }

    private fun createEntryDTO(
        id: Long,
        published: String,
        summary: String?,
        content: String?,
    ): EntryDTO = EntryDTO(
        id = id,
        feedId = 20,
        title = "Title",
        author = "Author",
        content = content,
        summary = summary,
        url = "https://example.com/article",
        extractedContentUrl = null,
        published = published,
        createdAt = "2023-11-14T10:00:00Z",
    )

    private class FakeHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String? = "Parsed Summary"
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
    }

    private class FakeDateFormatter : DateFormatter {
        var lastMillis: Long? = null
            private set

        override fun getDateMillisFromString(dateString: String): Long? = null
        override fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String {
            lastMillis = millis
            return "formatted-date"
        }
        override fun formatDateForLastRefresh(millis: Long): String = "formatted-refresh"
        override fun currentTimeMillis(): Long = 0
        override fun getCurrentDateForExport(): String = "2023-11-14"
    }
}
