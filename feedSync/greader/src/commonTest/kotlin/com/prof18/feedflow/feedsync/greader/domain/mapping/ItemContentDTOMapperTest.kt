package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.feedsync.greader.data.dto.ItemContentDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ItemContentDTOMapperTest {

    private val fakeHtmlParser = FakeHtmlParser()
    private val fakeDateFormatter = FakeDateFormatter()
    private val mapper = ItemContentDTOMapper(fakeHtmlParser, fakeDateFormatter)

    private val testFeedSource = FeedSource(
        id = "feed-1",
        url = "https://example.com/feed.xml",
        title = "Test Feed",
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
    fun `mapToFeedItem returns FeedItem with correct values when all fields present`() {
        val itemContentDTO = createItemContentDTO(
            id = "tag:google.com,2005:reader/item/abc123",
            published = 1700000000L,
            title = "Test Article",
            canonicalHref = "https://example.com/article",
            contentText = "<p>Article content</p>",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("abc123", result?.id)
        assertEquals("https://example.com/article", result?.url)
        assertEquals("Test Article", result?.title)
        assertEquals("<p>Article content</p>", result?.subtitle)
        assertEquals(testFeedSource, result?.feedSource)
        assertEquals(1700000000000L, result?.pubDateMillis)
        assertEquals("formatted-date", result?.dateString)
    }

    @Test
    fun `mapToFeedItem returns null when canonical URL is missing`() {
        val itemContentDTO = createItemContentDTO(
            id = "tag:google.com,2005:reader/item/abc123",
            published = 1700000000L,
            canonicalHref = null,
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertNull(result)
    }

    @Test
    fun `mapToFeedItem uses content when available`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            contentText = "Content text",
            summaryText = "Summary text",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("Content text", result?.subtitle)
    }

    @Test
    fun `mapToFeedItem uses summary when content is not available`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            contentText = null,
            summaryText = "Summary text",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("Summary text", result?.subtitle)
    }

    @Test
    fun `mapToFeedItem extracts image from enclosure when available`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            imageHref = "https://example.com/image.jpg",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("https://example.com/image.jpg", result?.imageUrl)
    }

    @Test
    fun `mapToFeedItem extracts image from content when enclosure is not available`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            imageHref = null,
            contentText = "Some text <img src=\"https://example.com/content-image.png\"> more text",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("https://example.com/content-image.png", result?.imageUrl)
    }

    @Test
    fun `mapToFeedItem filters out emoji images from content`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            imageHref = null,
            contentText = "Some text https://s.w.org/images/core/emoji/test.png more text",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertNull(result?.imageUrl)
    }

    @Test
    fun `mapToFeedItem sets isRead to true when read category is present`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            categories = listOf("user/-/state/com.google/read"),
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals(result?.isRead, true)
    }

    @Test
    fun `mapToFeedItem sets isRead to false when read category is not present`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            categories = emptyList(),
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertNotEquals(result?.isRead, true)
    }

    @Test
    fun `mapToFeedItem sets isBookmarked to true when starred category is present`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            categories = listOf("user/-/state/com.google/starred"),
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals(result?.isBookmarked, true)
    }

    @Test
    fun `mapToFeedItem sets isBookmarked to false when starred category is not present`() {
        val itemContentDTO = createItemContentDTO(
            canonicalHref = "https://example.com/article",
            categories = emptyList(),
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertNotEquals(result?.isBookmarked, true)
    }

    @Test
    fun `mapToFeedItem extracts hexID correctly from full id`() {
        val itemContentDTO = createItemContentDTO(
            id = "tag:google.com,2005:reader/item/0000000deadbeef",
            canonicalHref = "https://example.com/article",
        )

        val result = mapper.mapToFeedItem(itemContentDTO, testFeedSource)

        assertEquals("0000000deadbeef", result?.id)
    }

    private fun createItemContentDTO(
        id: String = "tag:google.com,2005:reader/item/default123",
        published: Long = 1700000000L,
        title: String? = "Default Title",
        canonicalHref: String? = null,
        contentText: String? = null,
        summaryText: String? = null,
        imageHref: String? = null,
        categories: List<String>? = null,
    ): ItemContentDTO {
        return ItemContentDTO(
            id = id,
            published = published,
            title = title,
            canonical = canonicalHref?.let { listOf(ItemContentDTO.Link(href = it)) },
            content = contentText?.let { ItemContentDTO.Content(content = it) },
            summary = summaryText?.let { ItemContentDTO.Summary(content = it) },
            origin = ItemContentDTO.Origin(
                streamId = "feed/https://example.com/feed.xml",
                htmlUrl = "https://example.com",
                title = "Test Origin",
            ),
            enclosure = imageHref?.let {
                listOf(ItemContentDTO.Enclosure(href = it, type = "image/jpeg"))
            },
            categories = categories,
        )
    }

    private class FakeHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String? = html
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
    }

    private class FakeDateFormatter : DateFormatter {
        override fun getDateMillisFromString(dateString: String): Long? = null
        override fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String =
            "formatted-date"
        override fun formatDateForLastRefresh(millis: Long): String = "formatted-refresh"
        override fun currentTimeMillis(): Long = 1700000000000L
        override fun getCurrentDateForExport(): String = "2023-11-14"
    }
}
