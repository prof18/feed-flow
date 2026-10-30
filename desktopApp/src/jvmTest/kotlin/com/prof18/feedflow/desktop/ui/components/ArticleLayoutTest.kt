package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedItem
import com.prof18.feedflow.core.model.FeedItemDisplaySettings
import com.prof18.feedflow.core.model.FeedItemUrlInfo
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.FeedSource
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ArticleLayoutTest(
    private val layout: FeedLayout,
    private val themeMode: ThemeMode,
) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `long article content fits row and metadata remains readable`() {
        var panelBackground = Color.Transparent
        val settings = mutableStateOf(FeedItemDisplaySettings())
        setRow(
            item = article.copy(isRead = true, isBookmarked = true),
            layout = layout,
            themeMode = themeMode,
            displaySettings = settings,
            onPanelBackground = { panelBackground = it },
        )

        rule.onNodeWithText(LONG_TITLE).assertIsDisplayed()
        rule.onNodeWithText(SOURCE_NAME).assertIsDisplayed()
        rule.onNodeWithText(PUBLISHED_DATE).assertIsDisplayed()
        rule.onNodeWithTag("article_bookmarked_${article.id}", useUnmergedTree = true).assertIsDisplayed()
        assertEquals(
            EnFeedFlowStrings.articleBookmarkedContentDescription,
            rule.onNodeWithTag(ROW_TAG).fetchSemanticsNode().config[SemanticsProperties.StateDescription],
        )
        assertNodeFitsRow(LONG_TITLE)

        val sourceColor = textColor(SOURCE_NAME)
        val dateColor = textColor(PUBLISHED_DATE)
        assertTrue(
            contrastRatio(sourceColor, panelBackground) >= MIN_METADATA_CONTRAST,
            "Source contrast was ${contrastRatio(sourceColor, panelBackground)} for $layout/$themeMode",
        )
        assertTrue(
            contrastRatio(dateColor, panelBackground) >= MIN_METADATA_CONTRAST,
            "Date contrast was ${contrastRatio(dateColor, panelBackground)} for $layout/$themeMode",
        )

        listOf(LONG_TITLE, DESCRIPTION).forEach { text ->
            val contrast = contrastRatio(textColor(text), panelBackground)
            assertTrue(contrast >= MIN_METADATA_CONTRAST, "Read text contrast was $contrast for $layout/$themeMode")
        }

        rule.runOnIdle { settings.value = settings.value.copy(isHideFeedSourceEnabled = true) }
        rule.onNodeWithText(SOURCE_NAME).assertDoesNotExist()
    }

    @Test
    fun `read titles recede clearly from unread ones`() {
        var panelBackground = Color.Transparent
        val item = mutableStateOf(article)
        setRow(item = item, layout = layout, themeMode = themeMode, onPanelBackground = { panelBackground = it })
        val unread = contrastRatio(textColor(LONG_TITLE), panelBackground)

        rule.runOnIdle { item.value = article.copy(isRead = true) }
        rule.waitForIdle()
        val read = contrastRatio(textColor(LONG_TITLE), panelBackground)

        assertTrue(
            read <= unread * MAX_READ_TO_UNREAD_CONTRAST,
            "Read title contrast $read is too close to unread $unread for $layout/$themeMode",
        )
    }

    @Test
    fun `empty title and missing media still leave a clickable row`() {
        val opened = mutableListOf<FeedItemUrlInfo>()
        val item = article.copy(title = "", subtitle = null, content = null, imageUrl = null)
        setRow(item = item, layout = layout, themeMode = themeMode, onFeedItemClick = opened::add)

        rule.onNodeWithTag(ROW_TAG).assertIsDisplayed()
        rule.onNodeWithTag(ROW_TAG).performClick()
        rule.waitForIdle()
        assertEquals(1, opened.size)
        assertEquals(
            FeedItemUrlInfo(
                id = item.id,
                url = item.url,
                title = item.title,
                isBookmarked = item.isBookmarked,
                articleOpenMode = item.feedSource.articleOpenMode,
                commentsUrl = item.commentsUrl,
                imageUrl = item.imageUrl,
                feedSourceTitle = item.feedSource.title,
            ),
            opened.single(),
        )
        val bounds = rule.onNodeWithTag(ROW_TAG).fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width > 0f && bounds.height > 0f, "Row had empty bounds for $layout/$themeMode: $bounds")
        assertTrue(
            bounds.width <= rule.onNodeWithTag(PARENT_TAG).fetchSemanticsNode().boundsInRoot.width + 1f,
            "Row exceeded its 320dp parent: $bounds",
        )
    }

    private fun setRow(
        item: FeedItem,
        layout: FeedLayout,
        themeMode: ThemeMode,
        displaySettings: State<FeedItemDisplaySettings> = mutableStateOf(FeedItemDisplaySettings()),
        onFeedItemClick: (FeedItemUrlInfo) -> Unit = {},
        onPanelBackground: (Color) -> Unit = {},
    ) = setRow(mutableStateOf(item), layout, themeMode, displaySettings, onFeedItemClick, onPanelBackground)

    private fun setRow(
        item: State<FeedItem>,
        layout: FeedLayout,
        themeMode: ThemeMode,
        displaySettings: State<FeedItemDisplaySettings> = mutableStateOf(FeedItemDisplaySettings()),
        onFeedItemClick: (FeedItemUrlInfo) -> Unit = {},
        onPanelBackground: (Color) -> Unit = {},
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(
                    darkTheme = themeMode != ThemeMode.LIGHT,
                    useOledTheme = themeMode == ThemeMode.OLED,
                ) {
                    onPanelBackground(JewelTheme.globalColors.panelBackground)
                    Box(Modifier.width(ROW_WIDTH).testTag(PARENT_TAG)) {
                        ArticleRow(
                            feedItem = item.value,
                            feedFontSize = FeedFontSizes(),
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            feedLayout = layout,
                            currentFeedFilter = FeedFilter.Timeline,
                            feedItemDisplaySettings = displaySettings.value,
                            onFeedItemClick = onFeedItemClick,
                            onOpenInBrowser = {},
                            onBookmarkClick = { _, _ -> },
                            onReadStatusClick = { _, _ -> },
                            onCommentClick = {},
                            onOpenFeedSettings = {},
                            onOpenFeedWebsite = {},
                            onShareClick = {},
                            onMarkAllAboveAsRead = {},
                            onMarkAllBelowAsRead = {},
                            modifier = Modifier.testTag(ROW_TAG),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun assertNodeFitsRow(text: String) {
        val bounds = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width > 0f, "Text had zero width for $layout/$themeMode")
        assertTrue(
            bounds.width <= rule.onNodeWithTag(PARENT_TAG).fetchSemanticsNode().boundsInRoot.width + 1f,
            "Text exceeded its row for $layout/$themeMode: $bounds",
        )
    }

    private fun textColor(text: String): Color {
        var result: androidx.compose.ui.text.TextLayoutResult? = null
        // The row merges its texts for accessibility; read the exact text node, not the merged row.
        rule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                action(layouts)
                result = layouts.single()
            }
        return requireNotNull(result).layoutInput.style.color
    }

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val composite = foreground.compositeOver(background)
        val first = composite.luminance().toDouble()
        val second = background.luminance().toDouble()
        return (maxOf(first, second) + LUMINANCE_OFFSET) / (minOf(first, second) + LUMINANCE_OFFSET)
    }

    companion object {
        private val ROW_WIDTH = 320.dp
        private const val PARENT_TAG = "article_layout_parent"
        private const val ROW_TAG = "article_layout_row"
        private const val SOURCE_NAME = "Example Source"
        private const val PUBLISHED_DATE = "October 3, 2026"
        private const val LONG_TITLE = "A long article headline that should wrap cleanly inside the " +
            "constrained desktop row without forcing the layout wider than its parent"
        private const val LUMINANCE_OFFSET = 0.05
        private const val MIN_METADATA_CONTRAST = 4.5

        // A read title must have at most 60% of the unread title's contrast, so the two are easy to tell apart.
        private const val MAX_READ_TO_UNREAD_CONTRAST = 0.6
        private const val DESCRIPTION =
            "A detailed description that also needs to remain within the measured row width."

        @JvmStatic
        @Parameterized.Parameters(name = "{0}/{1}")
        fun parameters(): List<Array<Any>> = FeedLayout.entries.flatMap { layout ->
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).map { mode -> arrayOf(layout, mode) }
        }

        private val article = FeedItem(
            id = "layout-test",
            url = "https://example.com/article",
            title = LONG_TITLE,
            subtitle = DESCRIPTION,
            content = null,
            imageUrl = null,
            feedSource = FeedSource(
                id = "source",
                url = "https://example.com/feed.xml",
                title = SOURCE_NAME,
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
            dateString = PUBLISHED_DATE,
            commentsUrl = null,
            isBookmarked = false,
        )
    }
}
