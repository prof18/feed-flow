package com.prof18.feedflow.android.ui.home.components.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.prof18.feedflow.android.ui.preview.feedItemsForPreview
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedItemViewAccessibilityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun listRowAnnouncesUnreadBookmarkedOnce() {
        setRow(FeedLayout.LIST, isRead = false, isBookmarked = true)

        assertRowState(EnFeedFlowStrings.articleUnreadBookmarkedState)
        assertNoBookmarkIconDescription()
    }

    @Test
    fun imageCardAnnouncesUnreadBookmarkedOnce() {
        setRow(FeedLayout.BIG_IMAGE, isRead = false, isBookmarked = true)

        assertRowState(EnFeedFlowStrings.articleUnreadBookmarkedState)
        assertNoBookmarkIconDescription()
    }

    @Test
    fun listRowAnnouncesUnreadWhenDotIsHidden() {
        setRow(
            FeedLayout.LIST,
            isRead = false,
            isBookmarked = false,
            displaySettings = FeedItemDisplaySettings(isHideUnreadDotEnabled = true),
        )

        assertRowState(EnFeedFlowStrings.articleUnreadState)
    }

    @Test
    fun imageCardAnnouncesUnreadWhenDotIsHidden() {
        setRow(
            FeedLayout.BIG_IMAGE,
            isRead = false,
            isBookmarked = false,
            displaySettings = FeedItemDisplaySettings(isHideUnreadDotEnabled = true),
        )

        assertRowState(EnFeedFlowStrings.articleUnreadState)
    }

    @Test
    fun readListRowHasNoState() {
        setRow(FeedLayout.LIST, isRead = true, isBookmarked = false)

        assertRowState(null)
    }

    @Test
    fun readImageCardHasNoState() {
        setRow(FeedLayout.BIG_IMAGE, isRead = true, isBookmarked = false)

        assertRowState(null)
    }

    private fun assertRowState(expected: String?) {
        val config = compose.onNodeWithTag(FeedItemE2eIds.row(ITEM.id)).fetchSemanticsNode().config
        val state = config.getOrNull(SemanticsProperties.StateDescription)
        if (expected == null) {
            assertNull(state)
        } else {
            assertEquals(expected, state)
        }
    }

    private fun assertNoBookmarkIconDescription() {
        compose
            .onAllNodesWithContentDescription(
                EnFeedFlowStrings.articleBookmarkedContentDescription,
                useUnmergedTree = true,
            )
            .assertCountEquals(0)
    }

    private fun setRow(
        layout: FeedLayout,
        isRead: Boolean,
        isBookmarked: Boolean,
        displaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
    ) {
        setContent {
            FeedItemView(
                feedItem = ITEM.copy(isRead = isRead, isBookmarked = isBookmarked),
                feedFontSize = FeedFontSizes(),
                shareMenuLabel = "Share",
                shareCommentsMenuLabel = "Share comments",
                feedLayout = layout,
                onFeedItemClick = {},
                onBookmarkClick = { _, _ -> },
                onReadStatusClick = { _, _ -> },
                onCommentClick = {},
                onOpenFeedSettings = {},
                onOpenFeedWebsite = {},
                onShareClick = {},
                onMarkAllAboveAsRead = {},
                onMarkAllBelowAsRead = {},
                feedItemDisplaySettings = displaySettings,
            )
        }
    }

    private fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { content() }
            }
        }
    }

    private companion object {
        val ITEM = feedItemsForPreview.first().copy(audioUrl = null, imageUrl = null)
    }
}
