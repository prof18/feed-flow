package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.SearchFilter
import com.prof18.feedflow.core.model.SearchState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SearchUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `query edit and clear update existing search callback and filter change retains query`() {
        val query = mutableStateOf("")
        val filter = mutableStateOf(SearchFilter.All)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false) {
                    DesktopSearchScreenContent(
                        searchState = SearchState.EmptyState,
                        searchQuery = query.value,
                        searchFilter = filter.value,
                        currentFeedFilter = FeedFilter.Timeline,
                        feedFontSizes = FeedFontSizes(),
                        shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                        shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                        updateSearchQuery = { query.value = it },
                        onSearchFilterSelected = { filter.value = it },
                        navigateBack = {},
                        onFeedItemClick = {},
                        onBookmarkClick = { _, _ -> },
                        onReadStatusClick = { _, _ -> },
                        onMarkAllAboveAsRead = {},
                        onMarkAllBelowAsRead = {},
                        onCommentClick = {},
                        onShareClick = {},
                        onOpenFeedSettings = {},
                        onOpenFeedWebsite = {},
                    )
                }
            }
        }
        rule.onNodeWithTag("search-query").performTextReplacement("Compose")
        rule.onNodeWithText(EnFeedFlowStrings.searchFilterBookmarks).performClick()
        rule.runOnIdle {
            assertEquals("Compose", query.value)
            assertEquals(SearchFilter.Bookmarks, filter.value)
        }
        rule.onNodeWithTag("search-clear").performClick()
        rule.runOnIdle { assertEquals("", query.value) }
    }

    @Test
    fun `done key clears focus from the search query`() {
        val query = mutableStateOf("")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false) {
                    DesktopSearchScreenContent(
                        searchState = SearchState.EmptyState,
                        searchQuery = query.value,
                        searchFilter = SearchFilter.All,
                        currentFeedFilter = FeedFilter.Timeline,
                        feedFontSizes = FeedFontSizes(),
                        shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                        shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                        updateSearchQuery = { query.value = it },
                        onSearchFilterSelected = {},
                        navigateBack = {},
                        onFeedItemClick = {},
                        onBookmarkClick = { _, _ -> },
                        onReadStatusClick = { _, _ -> },
                        onMarkAllAboveAsRead = {},
                        onMarkAllBelowAsRead = {},
                        onCommentClick = {},
                        onShareClick = {},
                        onOpenFeedSettings = {},
                        onOpenFeedWebsite = {},
                    )
                }
            }
        }

        val searchQuery = rule.onNodeWithTag("search-query")
        rule.waitForIdle()
        searchQuery.assertIsFocused()
        searchQuery.performTextReplacement("Compose")
        searchQuery.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        searchQuery.assertIsNotFocused()
    }
}
