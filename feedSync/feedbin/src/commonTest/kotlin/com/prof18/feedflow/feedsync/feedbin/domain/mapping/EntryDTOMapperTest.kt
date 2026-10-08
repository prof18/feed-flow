package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.feedsync.feedbin.data.dto.EntryDTO
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
        articleOpenMode = ArticleOpenMode.DEFAULT,
        isHiddenFromTimeline = false,
        isPinned = false,
        isNotificationEnabled = false,
        isHideImagesEnabled = false,
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
        assertEquals("<p>Content</p> https://example.com/image.jpg", result.content)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertEquals(true, result.isRead)
        assertEquals(false, result.isBookmarked)
        assertEquals("formatted-date", result.dateString)
        assertEquals(Instant.parse(published).toEpochMilliseconds() / 1000, dateFormatter.lastMillis)
    }

    @Test
    fun `wire enclosure keys deserialize and map audio while preserving article url`() {
        val entryDTO = Json.decodeFromString<EntryDTO>(ENTRY_WITH_ENCLOSURE_JSON)

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertEquals("https://example.com/article", result.url)
        assertEquals("https://cdn.example.com/episode?signature=a%2Fb", result.audioUrl)
    }

    @Test
    fun `absent and null wire enclosures deserialize without audio`() {
        val enclosureProperty = Regex(
            """,\s*"enclosure"\s*:\s*\{.*?\}""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val absentEnclosureJson = ENTRY_WITH_ENCLOSURE_JSON.replace(enclosureProperty, "")
        val nullEnclosureJson = ENTRY_WITH_ENCLOSURE_JSON.replace(
            Regex(""""enclosure"\s*:\s*\{.*?\}""", RegexOption.DOT_MATCHES_ALL),
            "\"enclosure\": null",
        )
        val absentEnclosure = Json.decodeFromString<EntryDTO>(absentEnclosureJson)
        val nullEnclosure = Json.decodeFromString<EntryDTO>(nullEnclosureJson)

        assertNull(absentEnclosure.enclosure)
        assertNull(nullEnclosure.enclosure)
        assertNull(mapper.mapToFeedItem(absentEnclosure, feedSource, false, false).audioUrl)
        assertNull(mapper.mapToFeedItem(nullEnclosure, feedSource, false, false).audioUrl)
    }

    @Test
    fun `declared image and video enclosures do not become audio`() {
        for (mimeType in listOf("image/jpeg", "video/mp4")) {
            val entry = createEntryDTO(
                id = 42,
                published = "2023-11-14T10:00:00Z",
                summary = "Summary",
                content = null,
            ).copy(
                enclosure = EntryDTO.Enclosure(url = "https://example.com/episode.mp3", type = mimeType),
            )
            assertNull(mapper.mapToFeedItem(entry, feedSource, false, false).audioUrl)
        }
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
        assertEquals("Summary https://example.com/summary.png", result.content)
    }

    @Test
    fun `mapToFeedItem keeps the query string of the image extracted from content`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = null,
            content = "<img src=\"https://images.tagesschau.de/image/abc/16x9-big/schnieder-144.jpg?width=1920\">",
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertEquals(
            "https://images.tagesschau.de/image/abc/16x9-big/schnieder-144.jpg?width=1920",
            result.imageUrl,
        )
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

    @Test
    fun `mapToFeedItem extracts commentsUrl from content`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = "Summary",
            content = """<p>Body</p><a href="https://news.ycombinator.com/item?id=1">Comments</a>""",
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertEquals("https://news.ycombinator.com/item?id=1", result.commentsUrl)
    }

    @Test
    fun `mapToFeedItem extracts commentsUrl from summary when content is null`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = """<a href="https://example.com/article#comments">Comments</a>""",
            content = null,
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertEquals("https://example.com/article#comments", result.commentsUrl)
    }

    @Test
    fun `mapToFeedItem sets commentsUrl to null when content has no comments anchor`() {
        val entryDTO = createEntryDTO(
            id = 10,
            published = "2023-11-14T10:00:00Z",
            summary = "Summary",
            content = "<p>Body mentioning comments but with no anchor</p>",
        )

        val result = mapper.mapToFeedItem(
            entryDTO = entryDTO,
            feedSource = feedSource,
            isRead = false,
            isBookmarked = false,
        )

        assertNull(result.commentsUrl)
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

    private companion object {
        const val ENTRY_WITH_ENCLOSURE_JSON = """
            {
              "id": 42,
              "feed_id": 20,
              "title": "Episode title",
              "author": "Podcast host",
              "content": "<p>Episode notes</p>",
              "summary": "Summary",
              "url": "https://example.com/article",
              "extracted_content_url": null,
              "published": "2023-11-14T10:00:00Z",
              "created_at": "2023-11-14T10:00:00Z",
              "enclosure": {
                "enclosure_url": "https://cdn.example.com/episode?signature=a%2Fb",
                "enclosure_type": "audio/mpeg"
              }
            }
        """
    }

    private class FakeHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String? = "Parsed Summary"
        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
        override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent {
            val regex = Regex("""<a\s[^>]*href="([^"]*)"[^>]*>\s*[Cc]omments\s*</a>""")
            val commentsUrl = regex.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            return ParsedFeedContent(text = "Parsed Summary", commentsUrl = commentsUrl)
        }
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
