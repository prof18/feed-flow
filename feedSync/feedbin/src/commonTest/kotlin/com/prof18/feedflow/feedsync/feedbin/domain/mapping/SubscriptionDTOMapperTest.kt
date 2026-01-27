package com.prof18.feedflow.feedsync.feedbin.domain.mapping

import com.prof18.feedflow.feedsync.feedbin.data.dto.IconDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.feedbin.data.dto.TaggingDTO
import com.prof18.feedflow.feedsync.feedbin.domain.feedbinFeedSourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubscriptionDTOMapperTest {

    @Test
    fun `toFeedSource maps category and logo when data is available`() {
        val subscription = createSubscriptionDTO(
            id = 10,
            feedId = 20,
            siteUrl = "https://example.com/blog",
        )
        val taggings = listOf(
            TaggingDTO(
                id = 1,
                feedId = 20,
                name = "Tech",
            ),
        )
        val icons = listOf(
            IconDTO(
                host = "example.com",
                url = "https://example.com/favicon.png",
            ),
        )

        val result = subscription.toFeedSource(taggings = taggings, icons = icons)

        assertEquals(feedbinFeedSourceId(subscriptionId = 10, feedId = 20), result.id)
        assertEquals("https://example.com/feed.xml", result.url)
        assertEquals("Example", result.title)
        assertEquals("Tech", result.category?.id)
        assertEquals("https://example.com/favicon.png", result.logoUrl)
        assertEquals("https://example.com/blog", result.websiteUrl)
    }

    @Test
    fun `toFeedSource returns null category and logo when matching data is missing`() {
        val subscription = createSubscriptionDTO(
            id = 10,
            feedId = 20,
            siteUrl = "https://example.com/blog",
        )

        val result = subscription.toFeedSource(
            taggings = emptyList(),
            icons = emptyList(),
        )

        assertNull(result.category)
        assertNull(result.logoUrl)
    }

    @Test
    fun `toFeedSource skips logo when site url is invalid`() {
        val subscription = createSubscriptionDTO(
            id = 10,
            feedId = 20,
            siteUrl = "not-a-url",
        )
        val icons = listOf(
            IconDTO(
                host = "not-a-url",
                url = "https://example.com/favicon.png",
            ),
        )

        val result = subscription.toFeedSource(
            taggings = emptyList(),
            icons = icons,
        )

        assertNull(result.logoUrl)
    }

    private fun createSubscriptionDTO(
        id: Long,
        feedId: Long,
        siteUrl: String,
    ): SubscriptionDTO = SubscriptionDTO(
        id = id,
        createdAt = "2023-10-10T10:00:00Z",
        feedId = feedId,
        title = "Example",
        feedUrl = "https://example.com/feed.xml",
        siteUrl = siteUrl,
    )
}
