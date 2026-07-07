package com.prof18.feedflow.shared.domain.mappers

import com.prof18.feedflow.core.domain.DateFormatter
import com.prof18.feedflow.core.domain.HtmlParser
import com.prof18.feedflow.core.domain.ParsedFeedContent
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.test.generators.FeedSourceGenerator
import com.prof18.feedflow.shared.test.generators.RssChannelGenerator
import com.prof18.feedflow.shared.test.generators.RssItemGenerator
import com.prof18.feedflow.shared.test.testLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class RssChannelMapperTest {

    private val mapper = RssChannelMapper(
        dateFormatter = FakeDateFormatter(),
        htmlParser = FakeHtmlParser(),
        logger = testLogger,
    )

    @Test
    fun `getFeedItems stores content when content encoded is present`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    description = "<p>Short summary</p>",
                    content = "<article>Full feed content</article>",
                ),
            ),
        )

        val result = mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource())

        assertEquals("<article>Full feed content</article>", result.single().content)
        assertEquals("Short summary", result.single().subtitle)
    }

    @Test
    fun `getFeedItems falls back to description for content`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    description = "<p>Feed description</p>",
                    content = null,
                ),
            ),
        )

        val result = mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource())

        assertEquals("<p>Feed description</p>", result.single().content)
    }

    private class FakeHtmlParser : HtmlParser {
        override fun getTextFromHTML(html: String): String? = html
            .replace("<p>", "")
            .replace("</p>", "")

        override fun getFaviconUrl(html: String): String? = null
        override fun getRssUrl(html: String): String? = null
        override fun parseFeedContent(html: String, baseUrl: String?): ParsedFeedContent =
            ParsedFeedContent(text = html, commentsUrl = null)
    }

    private class FakeDateFormatter : DateFormatter {
        override fun getDateMillisFromString(dateString: String): Long? = 1_704_067_200_000L
        override fun formatDateForFeed(millis: Long, dateFormat: DateFormat, timeFormat: TimeFormat): String =
            "formatted-date"

        override fun formatDateForLastRefresh(millis: Long): String = "formatted-refresh"
        override fun currentTimeMillis(): Long = 1_704_067_200_000L
        override fun getCurrentDateForExport(): String = "2024-01-01"
    }
}
