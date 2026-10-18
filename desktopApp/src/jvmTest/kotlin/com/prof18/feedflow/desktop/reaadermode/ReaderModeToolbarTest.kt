package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleReportUiState
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderModeToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `Close dismisses the reader in split view without expanding it`() {
        var closed = 0
        var toggled = 0
        setToolbar(onClose = { closed++ }, onToggle = { toggled++ })
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).performClick()
        rule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(0, toggled)
        }
    }

    @Test
    fun `Close dismisses the expanded reader without changing layout`() {
        var closed = 0
        var toggled = 0
        setToolbar(expanded = { true }, onClose = { closed++ }, onToggle = { toggled++ })
        rule.onNodeWithContentDescription(EnFeedFlowStrings.closeButtonContentDescription).performClick()
        rule.runOnIdle {
            assertEquals(1, closed)
            assertEquals(0, toggled)
        }
    }

    @Test
    fun `reader expansion has distinct labels and never closes the article`() {
        val expanded = mutableStateOf(false)
        var closed = 0
        setToolbar(expanded = { expanded.value }, onClose = { closed++ }, onToggle = {
            expanded.value = !expanded.value
        })
        rule.onNodeWithContentDescription("Expand reader").performClick()
        rule.onNodeWithContentDescription("Restore article list").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Expand reader").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, closed) }
    }

    @Test
    fun `compact reader keeps every toolbar action inside its viewport`() {
        val state = createReaderState(false)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        setToolbar(
            readerState = state.copy(
                readerModeData = state.readerModeData.copy(
                    commentsUrl = "https://example.com/comments",
                    canToggleContentSource = true,
                ),
            ),
            direction = { direction.value },
        )
        val descriptions = listOf(
            EnFeedFlowStrings.closeButtonContentDescription,
            "Expand reader",
            EnFeedFlowStrings.menuAddToBookmark,
            EnFeedFlowStrings.readerContentSourceFeed,
            EnFeedFlowStrings.menuCopyLink,
            EnFeedFlowStrings.moreOptionsButtonContentDescription,
            EnFeedFlowStrings.readerModeCommentsButtonContentDescription,
            EnFeedFlowStrings.readerModeBrowserButtonContentDescription,
            EnFeedFlowStrings.readerModeTextSettings,
        )
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { layoutDirection ->
            rule.runOnIdle { direction.value = layoutDirection }
            val viewport = rule.onNodeWithTag("reader-toolbar-viewport").getUnclippedBoundsInRoot()
            descriptions.forEach { description ->
                val bounds = rule.onNodeWithContentDescription(description).assertIsDisplayed()
                    .getUnclippedBoundsInRoot()
                assertTrue(bounds.left >= viewport.left && bounds.right <= viewport.right, description)
                assertTrue(bounds.right - bounds.left >= 32.dp, "$description must retain its minimum target width")
            }
        }
    }

    @Test
    fun `single pane reader retains Back without a layout toggle`() {
        var closed = 0
        setToolbar(onClose = { closed++ }, onToggle = null, showBackButton = true)
        rule.onNodeWithContentDescription("Expand reader").assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.desktopSearchBackButton).performClick()
        rule.runOnIdle { assertEquals(1, closed) }
    }

    private fun setToolbar(
        readerState: ReaderModeState.Success = createReaderState(false),
        expanded: () -> Boolean = { false },
        onClose: () -> Unit = {},
        onToggle: (() -> Unit)? = {},
        showBackButton: Boolean = false,
        direction: () -> LayoutDirection = { LayoutDirection.Ltr },
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction(),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Box(Modifier.width(360.dp).testTag("reader-toolbar-viewport")) {
                        ReaderModeToolbar(
                            readerModeState = readerState,
                            fontSize = ReaderModeDefaults.FONT_SIZE,
                            navigateBack = onClose,
                            showBackButton = showBackButton,
                            onToggleDetailFullscreen = onToggle,
                            isDetailFullscreen = expanded(),
                            openInBrowser = {},
                            onShareClick = {},
                            onArchiveClick = {},
                            onCommentsClick = {},
                            onFontSizeChange = {},
                            lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                            onLineHeightChange = {},
                            isBookmarked = false,
                            onToggleBookmark = {},
                            onToggleContentSource = {},
                            articleReportState = ArticleReportUiState(),
                            onReportArticle = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `bookmark control exposes selection state and reports both toggle values`() {
        val readerState = mutableStateOf(createReaderState(isBookmarked = false))
        val bookmarkChanges = mutableListOf<Pair<FeedItemId, Boolean>>()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ReaderModeToolbar(
                        readerModeState = readerState.value,
                        fontSize = ReaderModeDefaults.FONT_SIZE,
                        navigateBack = {},
                        showBackButton = false,
                        onToggleDetailFullscreen = null,
                        isDetailFullscreen = false,
                        openInBrowser = {},
                        onShareClick = {},
                        onArchiveClick = {},
                        onCommentsClick = {},
                        onFontSizeChange = {},
                        lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                        onLineHeightChange = {},
                        isBookmarked = readerState.value.getIsBookmarked,
                        onToggleBookmark = {
                            val next = !readerState.value.getIsBookmarked
                            bookmarkChanges += FeedItemId(READER_ITEM_ID) to next
                            readerState.value = createReaderState(next)
                        },
                        onToggleContentSource = {},
                        articleReportState = ArticleReportUiState(),
                        onReportArticle = {},
                    )
                }
            }
        }

        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark).assertIsNotSelected()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuRemoveFromBookmark).assertIsSelected()

        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuRemoveFromBookmark).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuAddToBookmark).assertIsNotSelected()
        rule.runOnIdle {
            assertEquals(
                listOf(FeedItemId(READER_ITEM_ID) to true, FeedItemId(READER_ITEM_ID) to false),
                bookmarkChanges,
            )
        }
    }

    private companion object {
        const val READER_ITEM_ID = "reader-bookmark-test"
    }
}

private fun createReaderState(isBookmarked: Boolean) = ReaderModeState.Success(
    ReaderModeData(
        id = FeedItemId("reader-bookmark-test"),
        title = "Bookmark test article",
        content = "Article content",
        url = "https://example.com/article",
        baseUrl = "https://example.com",
        fontSize = ReaderModeDefaults.FONT_SIZE,
        lineHeight = ReaderModeDefaults.LINE_HEIGHT,
        isBookmarked = isBookmarked,
    ),
)
