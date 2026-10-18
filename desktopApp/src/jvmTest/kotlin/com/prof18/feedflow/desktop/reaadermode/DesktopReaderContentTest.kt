package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
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
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.ArticleReportUiState
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.FeedItemUrlInfo
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
import kotlin.test.assertTrue

class DesktopReaderContentTest {
    @get:Rule
    val rule = createComposeRule()

    private val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE).build()

    @Test
    fun `loading transitions to actual Jewel content without retaining the spinner`() {
        val state = mutableStateOf<ReaderModeState>(ReaderModeState.Loading)
        rule.mainClock.autoAdvance = false
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
                        loadingArticle = LOADING_ARTICLE,
                    )
                }
            }
        }
        // The selected article's header shows at once; the spinner waits so quick loads never flash.
        var frames = 0
        while (frames < HEADER_FRAME_BUDGET && !hasText(LOADING_ARTICLE.title!!)) {
            rule.mainClock.advanceTimeByFrame()
            Thread.sleep(10)
            frames++
        }
        assertTrue(hasText(LOADING_ARTICLE.title!!), "The loading header must show the selected article")
        rule.onNodeWithTag(READER_LOADING_TAG).assertDoesNotExist()
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag(READER_LOADING_TAG).assertIsDisplayed()

        rule.mainClock.autoAdvance = true
        rule.runOnIdle { state.value = success() }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(BODY_TEXT).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText(BODY_TEXT).assertIsDisplayed()
        rule.onNodeWithTag(READER_LOADING_TAG).assertDoesNotExist()
    }

    private fun hasText(text: String): Boolean =
        rule.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

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

    @Test
    fun `inline svg markup is left out instead of shown as text`() {
        val content = "$BODY_TEXT\n\n<svg version=\"1.1\" viewBox=\"0 0 10 10\"><path d=\"M0,0\"></path></svg>" +
            "\n\nAfter the logo."
        val state = success()
        showBody(state.copy(readerModeData = state.readerModeData.copy(content = content)))
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("After the logo.", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText(BODY_TEXT, substring = true).assertIsDisplayed()
        rule.onAllNodesWithText("<svg", substring = true, useUnmergedTree = true).assertCountEquals(0)
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
                        isBookmarked = false,
                        onToggleBookmark = {},
                        onToggleContentSource = onToggleContentSource,
                        articleReportState = ArticleReportUiState(),
                        onReportArticle = {},
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

// Fewer than 300ms of frames, so the header must appear before the spinner delay elapses.
private const val HEADER_FRAME_BUDGET = 15

private val LOADING_ARTICLE = FeedItemUrlInfo(
    id = "loading-article",
    url = "https://example.com/loading",
    title = "Loading article title",
    isBookmarked = false,
    articleOpenMode = ArticleOpenMode.DEFAULT,
    commentsUrl = null,
    feedSourceTitle = "Example feed",
)
