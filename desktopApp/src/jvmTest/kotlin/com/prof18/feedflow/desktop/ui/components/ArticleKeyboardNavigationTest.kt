package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class ArticleKeyboardNavigationTest(private val layout: FeedLayout) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `arrow navigation scrolls to articles and actions retain the intended target`() {
        val opened = mutableListOf<FeedItemUrlInfo>()
        val bookmarks = mutableListOf<Pair<FeedItemId, Boolean>>()
        val websites = mutableListOf<String>()
        val feedSettings = mutableListOf<String>()
        var gridArrangement: Boolean? = null
        val items = (0 until ITEM_COUNT).map { index ->
            ArticleRowTest.article.copy(
                id = "keyboard_$index",
                url = "https://example.com/article/$index",
                title = "Keyboard article $index",
                feedSource = ArticleRowTest.article.feedSource.copy(
                    id = "source_$index",
                    websiteUrl = "https://example.com/source/$index",
                ),
            )
        }.toImmutableList()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    Box(Modifier.size(width = 720.dp, height = 280.dp)) {
                        DesktopArticleList(
                            feedItems = items,
                            nextFeedState = NextFeedDisplayState.NextFeedDisplayDisabledState,
                            feedFontSize = FeedFontSizes(),
                            feedLayout = layout,
                            currentFeedFilter = FeedFilter.Timeline,
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            onVisibleFeedItemsChanged = {},
                            requestMoreItems = {},
                            onFeedItemClick = opened::add,
                            onOpenInBrowser = {},
                            onBookmarkClick = { id, value -> bookmarks += id to value },
                            onReadStatusClick = { _, _ -> },
                            onCommentClick = {},
                            markAllAsRead = {},
                            onShareClick = {},
                            onOpenFeedSettings = { feedSettings += it.id },
                            onOpenFeedWebsite = websites::add,
                            onNavigateNext = {},
                            onGridArrangementChanged = { gridArrangement = it },
                            showFooter = false,
                        )
                    }
                }
            }
        }

        rule.runOnIdle { assertEquals(layout == FeedLayout.GRID, gridArrangement) }
        fun row(index: Int) = rule.onNodeWithTag("article_row_keyboard_$index")
        row(0).requestFocus()
        row(0).performKeyInput { pressKey(Key.DirectionUp) }
        row(0).assertIsDisplayed().assertIsFocused()
        for (index in 1 until ITEM_COUNT) {
            row(index - 1).performKeyInput { pressKey(Key.DirectionDown) }
            row(index).assertIsDisplayed().assertIsFocused()
        }
        val last = row(ITEM_COUNT - 1)
        last.performKeyInput { pressKey(Key.DirectionDown) }
        last.assertIsDisplayed().assertIsFocused()
        last.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(listOf(items.last().id), opened.map { it.id }) }

        last.performKeyInput { pressKey(Key.Tab) }
        val bookmark = rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark)
        bookmark.assertIsDisplayed().assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        rule.runOnIdle {
            assertEquals(listOf(FeedItemId(items.last().id) to true), bookmarks)
            assertEquals(listOf(items.last().id), opened.map { it.id })
        }
        bookmark.performKeyInput {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Tab)
            keyUp(Key.ShiftLeft)
        }
        last.assertIsFocused()

        last.performKeyInput { pressKey(Key.Menu) }
        val settings = rule.onNodeWithText(EnFeedFlowStrings.openFeedSettings)
        settings.assertIsDisplayed().performKeyInput { pressKey(Key.DirectionDown) }
        settings.assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        val website = rule.onNodeWithText(EnFeedFlowStrings.openFeedWebsiteButton)
        website.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        settings.assertDoesNotExist()
        last.assertIsFocused()
        rule.runOnIdle {
            assertEquals(listOf(items.last().feedSource.websiteUrl), websites)
            assertEquals(listOf(items.last().id), opened.map { it.id })
            assertEquals(emptyList(), feedSettings)
            assertEquals(listOf(FeedItemId(items.last().id) to true), bookmarks)
        }

        for (index in ITEM_COUNT - 2 downTo 0) {
            row(index + 1).performKeyInput { pressKey(Key.DirectionUp) }
            row(index).assertIsDisplayed().assertIsFocused()
        }
    }

    companion object {
        private const val ITEM_COUNT = 12

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun layouts(): List<Array<FeedLayout>> = listOf(FeedLayout.LIST, FeedLayout.GRID).map { arrayOf(it) }
    }
}
