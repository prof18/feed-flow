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

    @Test
    fun `getFeedItems falls back to description when content is blank`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    description = "<p>Feed description</p>",
                    content = "   ",
                ),
            ),
        )

        val result = mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource())

        assertEquals("<p>Feed description</p>", result.single().content)
    }

    @Test
    fun `getFeedItems treats a blank link as missing`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = "not-a-url",
                    link = "   ",
                    description = null,
                    content = null,
                ),
            ),
        )

        assertEquals(0, mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource()).size)
    }

    @Test
    fun `getFeedItems keeps item without url when content is present`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = "no-url-guid",
                    link = null,
                    description = null,
                    content = "<article>Full feed content without a link</article>",
                ),
            ),
        )

        val result = mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource())

        assertEquals(1, result.size)
        assertEquals("", result.single().url)
        assertEquals("<article>Full feed content without a link</article>", result.single().content)
    }

    @Test
    fun `getFeedItems skips item without url and without content`() {
        val rssChannel = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = "no-url-no-content-guid",
                    link = null,
                    description = null,
                    content = null,
                ),
            ),
        )

        val result = mapper.getFeedItems(rssChannel, FeedSourceGenerator.feedSource())

        assertEquals(0, result.size)
    }

    @Test
    fun `url-less items with identical content from different feeds have different ids`() {
        val item = RssItemGenerator.rssItem(
            guid = null,
            link = null,
            title = "Shared title",
            description = null,
            content = "<article>Identical syndicated content</article>",
        )
        val channel = RssChannelGenerator.rssChannel(items = listOf(item))

        val first = mapper.getFeedItems(
            channel,
            FeedSourceGenerator.feedSource(id = "first-feed"),
        ).single()
        val second = mapper.getFeedItems(
            channel,
            FeedSourceGenerator.feedSource(id = "second-feed"),
        ).single()

        kotlin.test.assertNotEquals(first.id, second.id)
    }

    @Test
    fun `url-less item with a guid keeps its id when the publisher edits it`() {
        val feedSource = FeedSourceGenerator.feedSource(id = "editing-feed")
        val original = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = "stable-guid",
                    link = null,
                    title = "Original title",
                    description = null,
                    content = "<article>Original body</article>",
                ),
            ),
        )
        val edited = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = "stable-guid",
                    link = null,
                    title = "Corrected title",
                    description = null,
                    content = "<article>Corrected body</article>",
                ),
            ),
        )

        assertEquals(
            mapper.getFeedItems(original, feedSource).single().id,
            mapper.getFeedItems(edited, feedSource).single().id,
        )
    }

    @Test
    fun `url-less item without a guid falls back to a content fingerprint`() {
        val feedSource = FeedSourceGenerator.feedSource(id = "fingerprint-feed")
        fun channelWithContent(content: String) = RssChannelGenerator.rssChannel(
            items = listOf(
                RssItemGenerator.rssItem(
                    guid = null,
                    link = null,
                    title = "Same title",
                    description = null,
                    content = content,
                ),
            ),
        )

        kotlin.test.assertNotEquals(
            mapper.getFeedItems(channelWithContent("<article>One</article>"), feedSource).single().id,
            mapper.getFeedItems(channelWithContent("<article>Two</article>"), feedSource).single().id,
        )
    }

    @Test
    fun `url-less items with identical guid from different feeds have different ids`() {
        val item = RssItemGenerator.rssItem(
            guid = "1",
            link = null,
            description = null,
            content = "<article>First feed's content</article>",
        )
        val channel = RssChannelGenerator.rssChannel(items = listOf(item))

        val first = mapper.getFeedItems(channel, FeedSourceGenerator.feedSource(id = "first-feed")).single()
        val second = mapper.getFeedItems(channel, FeedSourceGenerator.feedSource(id = "second-feed")).single()

        kotlin.test.assertNotEquals(first.id, second.id)
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
