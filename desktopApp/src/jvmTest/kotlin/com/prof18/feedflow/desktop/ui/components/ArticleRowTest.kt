package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class ArticleRowTest(private val articleLayout: FeedLayout) {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `row click opens article and right click bookmark action does not open it`() {
        val openedArticles = mutableListOf<FeedItemUrlInfo>()
        val bookmarks = mutableListOf<Pair<FeedItemId, Boolean>>()
        val readStatusChanges = mutableListOf<Pair<FeedItemId, Boolean>>()
        setRow(
            onFeedItemClick = openedArticles::add,
            onBookmarkClick = { id, bookmarked -> bookmarks += id to bookmarked },
            onReadStatusClick = { id, isRead -> readStatusChanges += id to isRead },
        )

        rule.onNodeWithTag(ARTICLE_ROW_TAG).assertIsDisplayed()
        rule.onNodeWithText(ARTICLE_TITLE).assertIsDisplayed()
        rule.onNodeWithTag(ARTICLE_ROW_TAG).performMouseInput { click() }
        rule.waitForIdle()
        assertEquals(1, openedArticles.size)
        assertEquals(ARTICLE_ID, openedArticles.single().id)

        rule.onNodeWithTag(ARTICLE_ROW_TAG).performMouseInput { click(button = MouseButton.Secondary) }
        rule.waitForIdle()
        rule.onNodeWithText(EnFeedFlowStrings.menuAddToBookmark).performMouseInput { click() }
        rule.waitForIdle()
        rule.onNodeWithTag(ARTICLE_ROW_TAG).performMouseInput { click(button = MouseButton.Secondary) }
        rule.onNodeWithText(EnFeedFlowStrings.menuMarkAsRead).performMouseInput { click() }
        rule.waitForIdle()

        assertEquals(listOf(FeedItemId(ARTICLE_ID) to true), bookmarks)
        assertEquals(listOf(FeedItemId(ARTICLE_ID) to true), readStatusChanges)
        assertEquals(1, openedArticles.size)
    }

    @Test
    fun `keyboard context menu exposes actions and Escape returns to the article`() {
        val openedArticles = mutableListOf<FeedItemUrlInfo>()
        setRow(onFeedItemClick = openedArticles::add)
        val row = rule.onNodeWithTag(ARTICLE_ROW_TAG)
        row.requestFocus()
        row.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.F10)
            keyUp(Key.ShiftLeft)
        }
        rule.onNodeWithText(EnFeedFlowStrings.menuAddToBookmark).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings).performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithText(EnFeedFlowStrings.menuAddToBookmark).assertDoesNotExist()
        row.assertIsFocused()
        assertEquals(0, openedArticles.size)
        row.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(ARTICLE_ID, openedArticles.single().id)
    }

    @Test
    fun `reader selection remains selected while keyboard focus enters and leaves the row`() {
        setRow(selected = true)
        val row = rule.onNodeWithTag(ARTICLE_ROW_TAG)
        row.assertIsSelected()
        row.requestFocus()
        row.assertIsFocused().assertIsSelected()
    }

    @Test
    fun `focused quick actions change the intended article without opening it`() {
        val opened = mutableListOf<FeedItemUrlInfo>()
        val bookmarks = mutableListOf<Pair<FeedItemId, Boolean>>()
        val readChanges = mutableListOf<Pair<FeedItemId, Boolean>>()
        setRow(
            onFeedItemClick = opened::add,
            onBookmarkClick = { id, value -> bookmarks += id to value },
            onReadStatusClick = { id, value -> readChanges += id to value },
        )
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark).assertDoesNotExist()
        rule.onNodeWithTag(ARTICLE_ROW_TAG).requestFocus()
        rule.onNodeWithTag(ARTICLE_ROW_TAG).performKeyInput { pressKey(Key.Tab) }
        val bookmark = rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark)
        bookmark.assertIsDisplayed().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        bookmark.performKeyInput { pressKey(Key.Tab) }
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuMarkAsRead)
            .assertIsDisplayed().assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(listOf(FeedItemId(ARTICLE_ID) to true), bookmarks)
        assertEquals(listOf(FeedItemId(ARTICLE_ID) to true), readChanges)
        assertEquals(0, opened.size)
    }

    @Test
    fun `bookmarked article retains its title as the accessible row label`() {
        setRow(bookmarked = true)
        val semantics = rule.onNodeWithTag(ARTICLE_ROW_TAG).fetchSemanticsNode().config
        assertNull(semantics.getOrNull(SemanticsProperties.ContentDescription))
        assertEquals(
            EnFeedFlowStrings.articleBookmarkedContentDescription,
            semantics[SemanticsProperties.StateDescription],
        )
        rule.onNodeWithText(ARTICLE_TITLE).assertIsDisplayed()
    }

    @Test
    fun `mark above confirmation cancels and accepts only once`() {
        val marked = mutableListOf<String>()
        setRow(onMarkAllAboveAsRead = marked::add)
        fun openConfirmation() {
            val row = rule.onNodeWithTag(ARTICLE_ROW_TAG)
            row.requestFocus()
            row.performKeyInput { pressKey(Key.Menu) }
            rule.onNodeWithText(EnFeedFlowStrings.menuMarkAllAboveAsRead)
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
        }
        openConfirmation()
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        assertEquals(emptyList(), marked)
        openConfirmation()
        val confirm = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            confirm()
            confirm()
        }
        rule.waitForIdle()
        assertEquals(listOf(ARTICLE_ID), marked)
    }

    private fun setRow(
        onFeedItemClick: (FeedItemUrlInfo) -> Unit = {},
        onBookmarkClick: (FeedItemId, Boolean) -> Unit = { _, _ -> },
        onReadStatusClick: (FeedItemId, Boolean) -> Unit = { _, _ -> },
        onMarkAllAboveAsRead: (String) -> Unit = {},
        layout: FeedLayout = articleLayout,
        selected: Boolean = false,
        bookmarked: Boolean = false,
        displaySettings: FeedItemDisplaySettings = FeedItemDisplaySettings(),
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Column {
                        ArticleRow(
                            feedItem = article.copy(isBookmarked = bookmarked),
                            isSelected = selected,
                            feedFontSize = FeedFontSizes(),
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            feedLayout = layout,
                            currentFeedFilter = FeedFilter.Timeline,
                            feedItemDisplaySettings = displaySettings,
                            onFeedItemClick = onFeedItemClick,
                            onOpenInBrowser = {},
                            onBookmarkClick = onBookmarkClick,
                            onReadStatusClick = onReadStatusClick,
                            onCommentClick = {},
                            onOpenFeedSettings = {},
                            onOpenFeedWebsite = {},
                            onShareClick = {},
                            onMarkAllAboveAsRead = onMarkAllAboveAsRead,
                            onMarkAllBelowAsRead = {},
                            modifier = Modifier.testTag(ARTICLE_ROW_TAG),
                        )
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun layouts(): List<Array<FeedLayout>> = FeedLayout.entries.map { arrayOf(it) }

        const val ARTICLE_ID = "test-row"
        const val ARTICLE_TITLE = "Row article"
        const val ARTICLE_DESCRIPTION = "A sufficiently long article summary used to verify the row description."
        const val ARTICLE_ROW_TAG = "article_row_test_row"

        val article = FeedItem(
            id = ARTICLE_ID,
            url = "https://example.com/article",
            title = ARTICLE_TITLE,
            subtitle = ARTICLE_DESCRIPTION,
            content = null,
            imageUrl = null,
            feedSource = FeedSource(
                id = "source",
                url = "https://example.com/feed.xml",
                title = "Example Feed",
                category = null,
                lastSyncTimestamp = null,
                logoUrl = null,
                websiteUrl = "https://example.com",
                fetchFailed = false,
                articleOpenMode = ArticleOpenMode.DEFAULT,
                isHiddenFromTimeline = false,
                isPinned = false,
                isNotificationEnabled = false,
                isHideImagesEnabled = false,
            ),
            pubDateMillis = null,
            isRead = false,
            dateString = "Today",
            commentsUrl = null,
            isBookmarked = false,
        )
    }
}
