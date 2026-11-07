package com.prof18.feedflow.shared.presentation.model

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.generators.FeedItemGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedItemAccessibilityTest : KoinTestBase() {
    @Test
    fun `unread article announces unread`() {
        val item = FeedItemGenerator.feedItem(isRead = false, isBookmarked = false)

        assertEquals(EnFeedFlowStrings.articleUnreadState, item.accessibilityState(EnFeedFlowStrings))
    }

    @Test
    fun `unread bookmarked article announces both`() {
        val item = FeedItemGenerator.feedItem(isRead = false, isBookmarked = true)

        assertEquals(EnFeedFlowStrings.articleUnreadBookmarkedState, item.accessibilityState(EnFeedFlowStrings))
    }

    @Test
    fun `read bookmarked article announces bookmarked`() {
        val item = FeedItemGenerator.feedItem(isRead = true, isBookmarked = true)

        assertEquals(
            EnFeedFlowStrings.articleBookmarkedContentDescription,
            item.accessibilityState(EnFeedFlowStrings),
        )
    }

    @Test
    fun `read article stays quiet`() {
        val item = FeedItemGenerator.feedItem(isRead = true, isBookmarked = false)

        assertNull(item.accessibilityState(EnFeedFlowStrings))
    }
}
