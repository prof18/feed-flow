package com.prof18.feedflow.feedsync.greader.domain.mapping

import com.prof18.feedflow.feedsync.greader.data.dto.SubscriptionDTO
import com.prof18.feedflow.feedsync.greader.data.dto.TagDTO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubscriptionDTOMapperTest {

    @Test
    fun `toFeedSource prefers url when present`() {
        val dto = createSubscriptionDTO(
            id = "feed/https://example.com/feed.xml",
            url = " https://example.com/custom.xml ",
        )

        val result = dto.toFeedSource()

        assertEquals("https://example.com/custom.xml", result.url)
    }

    @Test
    fun `toFeedSource uses id without feed prefix when url is missing`() {
        val dto = createSubscriptionDTO(
            id = "feed/https://example.com/feed.xml",
            url = null,
        )

        val result = dto.toFeedSource()

        assertEquals("https://example.com/feed.xml", result.url)
    }

    @Test
    fun `toFeedSource uses id when url is missing and no prefix`() {
        val dto = createSubscriptionDTO(
            id = "https://example.com/feed.xml",
            url = null,
        )

        val result = dto.toFeedSource()

        assertEquals("https://example.com/feed.xml", result.url)
    }

    @Test
    fun `toFeedSource extracts website host and handles empty icon`() {
        val dto = createSubscriptionDTO(
            id = "feed/https://example.com/feed.xml",
            url = null,
            htmlUrl = "https://example.com/path",
            iconUrl = "",
        )

        val result = dto.toFeedSource()

        assertEquals("https://example.com", result.websiteUrl)
        assertNull(result.logoUrl)
    }

    @Test
    fun `toFeedSource returns null category when list is empty`() {
        val dto = createSubscriptionDTO(
            id = "feed/https://example.com/feed.xml",
            categories = emptyList(),
        )

        val result = dto.toFeedSource()

        assertNull(result.category)
    }

    private fun createSubscriptionDTO(
        id: String,
        url: String? = "https://example.com/feed.xml",
        htmlUrl: String? = "https://example.com",
        iconUrl: String? = "https://example.com/icon.png",
        categories: List<TagDTO> = listOf(
            TagDTO(id = "user/-/label/Tech", label = "Tech"),
        ),
    ): SubscriptionDTO = SubscriptionDTO(
        id = id,
        title = "Example",
        categories = categories,
        url = url,
        htmlUrl = htmlUrl,
        iconUrl = iconUrl,
    )
}
