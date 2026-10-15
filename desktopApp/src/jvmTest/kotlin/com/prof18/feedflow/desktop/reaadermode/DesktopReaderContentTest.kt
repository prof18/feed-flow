package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import coil3.PlatformContext
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeData
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ShownContentSource
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.After
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopReaderContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()

    @Test
    fun `loading transitions to actual Jewel content without retaining the spinner`() {
        val state = mutableStateOf<ReaderModeState>(ReaderModeState.Loading)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = true, useOledTheme = false) {
                    DesktopReaderContent(
                        state = state.value,
                        fontSize = ReaderModeDefaults.FONT_SIZE,
                        lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                        imageLoader = imageLoader,
                        onOpenInBrowser = {},
                        onHoveredLinkChange = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        rule.onNodeWithTag(READER_LOADING_TAG).assertIsDisplayed()

        rule.runOnIdle { state.value = success() }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(BODY_TEXT).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText(BODY_TEXT).assertIsDisplayed()
        rule.onNodeWithTag(READER_LOADING_TAG).assertDoesNotExist()
    }

    @Test
    fun `unavailable URL-less content has an honest fallback without a browser action`() {
        showBody(ReaderModeState.ContentNotAvailable(url = "", id = "missing-body", isBookmarked = false))

        rule.onNodeWithText(EnFeedFlowStrings.readerModeNoContentTitle).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.readerModeFallbackOpenBrowserButton).assertDoesNotExist()
    }

    @Test
    fun `unavailable web content offers its actual browser destination`() {
        val opened = mutableListOf<String>()
        showBody(
            ReaderModeState.ContentNotAvailable(url = ARTICLE_URL, id = "web-fallback", isBookmarked = false),
            onOpenInBrowser = opened::add,
        )

        rule.onNodeWithText(EnFeedFlowStrings.readerModeFallbackTitle).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.readerModeFallbackOpenBrowserButton).performClick()
        rule.runOnIdle { assertEquals(listOf(ARTICLE_URL), opened) }
    }

    @Test
    fun `URL-less feed content renders while URL-specific toolbar actions stay hidden`() {
        showToolbar(success(url = ""))

        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerContentSourceFeed).assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeBrowserButtonContentDescription)
            .assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.menuShare).assertDoesNotExist()
        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeTextSettings).assertIsDisplayed()
    }

    @Test
    fun `eligible feed source toggle exposes the current source and invokes its callback`() {
        var toggles = 0
        showToolbar(success(), onToggleContentSource = { toggles++ })

        val source = rule.onNodeWithContentDescription(EnFeedFlowStrings.readerContentSourceFeed)
        source.assertIsSelected()
        source.performClick()
        rule.runOnIdle { assertEquals(1, toggles) }
    }

    @Test
    fun `ineligible source toggle remains absent when the shared state forbids it`() {
        showToolbar(success().copy(readerModeData = success().readerModeData.copy(canToggleContentSource = false)))
        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerContentSourceFeed).assertDoesNotExist()
    }

    private fun showBody(state: ReaderModeState, onOpenInBrowser: (String) -> Unit = {}) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    DesktopReaderContent(
                        state = state,
                        fontSize = ReaderModeDefaults.FONT_SIZE,
                        lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                        imageLoader = imageLoader,
                        onOpenInBrowser = onOpenInBrowser,
                        onHoveredLinkChange = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun showToolbar(state: ReaderModeState, onToggleContentSource: () -> Unit = {}) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ReaderModeToolbar(
                        readerModeState = state,
                        fontSize = ReaderModeDefaults.FONT_SIZE,
                        lineHeight = ReaderModeDefaults.LINE_HEIGHT,
                        navigateBack = {},
                        showBackButton = false,
                        onToggleDetailFullscreen = null,
                        isDetailFullscreen = false,
                        openInBrowser = {},
                        onShareClick = {},
                        onArchiveClick = {},
                        onCommentsClick = {},
                        onFontSizeChange = {},
                        onLineHeightChange = {},
                        onBookmarkClick = { _, _ -> },
                        onToggleContentSource = onToggleContentSource,
                    )
                }
            }
        }
    }

    @After
    fun closeImageLoader() { imageLoader.shutdown() }

    private fun success(url: String = ARTICLE_URL) = ReaderModeState.Success(
        ReaderModeData(
            id = FeedItemId("reader-content-test"),
            title = "Reader fixture",
            content = BODY_TEXT,
            url = url,
            baseUrl = "https://example.com",
            fontSize = ReaderModeDefaults.FONT_SIZE,
            lineHeight = ReaderModeDefaults.LINE_HEIGHT,
            isBookmarked = false,
            shownContentSource = ShownContentSource.FEED,
            canToggleContentSource = true,
        ),
    )

    private companion object {
        const val BODY_TEXT = "Content retained from the feed."
        const val ARTICLE_URL = "https://example.com/article"
    }
}
