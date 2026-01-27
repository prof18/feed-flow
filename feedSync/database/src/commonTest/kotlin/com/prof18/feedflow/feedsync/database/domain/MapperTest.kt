package com.prof18.feedflow.feedsync.database.domain

import com.prof18.feedflow.core.model.FeedSourceCategory
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.core.model.ParsedFeedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class MapperTest {

    @Test
    fun `toFeedSource maps parsed source and applies defaults`() {
        val parsed = ParsedFeedSource(
            id = "source-1",
            url = "https://example.com/feed.xml",
            title = "Example",
            category = FeedSourceCategory(id = "cat-1", title = "Tech"),
            logoUrl = "https://example.com/logo.png",
            websiteUrl = "https://example.com",
        )

        val result = parsed.toFeedSource()

        assertEquals("source-1", result.id)
        assertEquals("https://example.com/feed.xml", result.url)
        assertEquals("Example", result.title)
        assertEquals(parsed.category, result.category)
        assertEquals("https://example.com/logo.png", result.logoUrl)
        assertEquals("https://example.com", result.websiteUrl)
        assertNull(result.lastSyncTimestamp)
        assertEquals(LinkOpeningPreference.DEFAULT, result.linkOpeningPreference)
        assertFalse(result.fetchFailed)
        assertFalse(result.isHiddenFromTimeline)
        assertFalse(result.isPinned)
        assertFalse(result.isNotificationEnabled)
    }
}
