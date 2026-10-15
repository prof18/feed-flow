package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.core.model.FeedFontSizes
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeActions
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.shared.presentation.model.NextFeedDisplayState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class ArticleFooterLayoutTest(private val layout: FeedLayout) {
    @get:Rule
    val rule = createComposeRule()
    private val fontScale = mutableStateOf(2f)
    private val direction = mutableStateOf(LayoutDirection.Rtl)
    private val theme = mutableStateOf(ThemeMode.OLED)
    private val filter = mutableStateOf<FeedFilter>(FeedFilter.Timeline)
    private val markEnabled = mutableStateOf(true)
    private val showNext = mutableStateOf(true)

    @Test
    fun `mark all footer label fits at enlarged text size`() {
        setFooterContent()
        assertLabelFitsInAllConfigurations(EnFeedFlowStrings.markAllReadButton)
    }

    @Test
    fun `next source footer label fits at enlarged text size`() {
        setFooterContent()
        assertLabelFitsInAllConfigurations(NEXT_SOURCE_TITLE)
    }

    @Test
    fun `long next source footer label wraps at enlarged text size`() {
        setFooterContent(nextSourceTitle = LONG_SOURCE_TITLE)
        assertLabelFitsInAllConfigurations(LONG_SOURCE_TITLE)
    }

    @Test
    fun `footer retains keyboard actions disabled guard and read filter behavior`() {
        var marked = 0
        var navigated = 0
        setFooterContent(onMarkAll = { marked++ }, onNext = { navigated++ })
        val mark = rule.onNodeWithText(EnFeedFlowStrings.markAllReadButton)
        val next = rule.onNodeWithText(NEXT_SOURCE_TITLE)
        mark.requestFocus()
        mark.assertIsFocused().performKeyInput { pressKey(Key.Spacebar) }
        mark.performKeyInput { pressKey(Key.Tab) }
        next.assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle {
            assertEquals(1, marked)
            assertEquals(1, navigated)
            markEnabled.value = false
        }
        mark.assertIsNotEnabled().performClick()
        rule.runOnIdle {
            assertEquals(1, marked)
            filter.value = FeedFilter.Read
        }
        mark.assertDoesNotExist()
        next.assertIsDisplayed()
        rule.runOnIdle { showNext.value = false }
        next.assertDoesNotExist()
        mark.assertDoesNotExist()
    }

    private fun setFooterContent(
        nextSourceTitle: String = NEXT_SOURCE_TITLE,
        onMarkAll: () -> Unit = {},
        onNext: () -> Unit = {},
    ) {
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, fontScale.value),
                LocalLayoutDirection provides direction.value,
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    Box(
                        Modifier.requiredSize(
                            width = if (layout == FeedLayout.LIST) 360.dp else 720.dp,
                            height = 480.dp,
                        )
                            .clipToBounds().testTag(HOST_TAG),
                    ) {
                        DesktopArticleList(
                            feedItems = persistentListOf(ArticleRowTest.article.copy(subtitle = null)),
                            nextFeedState = NextFeedDisplayState.NextFeedDisplayEnabledState(nextSourceTitle),
                            feedFontSize = FeedFontSizes(),
                            feedLayout = layout,
                            currentFeedFilter = filter.value,
                            shareMenuLabel = EnFeedFlowStrings.menuCopyLink,
                            shareCommentsMenuLabel = EnFeedFlowStrings.menuCopyLinkComments,
                            swipeActions = SwipeActions(SwipeActionType.NONE, SwipeActionType.NONE),
                            onVisibleFeedItemsChanged = {},
                            requestMoreItems = {},
                            onFeedItemClick = {},
                            onOpenInBrowser = {},
                            onBookmarkClick = { _, _ -> },
                            onReadStatusClick = { _, _ -> },
                            onCommentClick = {},
                            markAllAsRead = onMarkAll,
                            onShareClick = {},
                            onOpenFeedSettings = {},
                            onOpenFeedWebsite = {},
                            onNavigateNext = onNext,
                            showNextFeedButton = showNext.value,
                            markAllAsReadEnabled = markEnabled.value,
                        )
                    }
                }
            }
        }
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
    }

    private fun assertLabelFitsInAllConfigurations(label: String) {
        for (mode in listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)) {
            for (scale in listOf(1f, 2f)) {
                for (layoutDirection in LayoutDirection.entries) {
                    rule.runOnIdle {
                        theme.value = mode
                        fontScale.value = scale
                        direction.value = layoutDirection
                    }
                    rule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
                    assertLabelFits(label)
                }
            }
        }
    }

    private fun assertLabelFits(label: String) {
        val node = rule.onNodeWithText(label)
        node.assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow, "$layout footer label must not clip: $label")
        val host = rule.onNodeWithTag(HOST_TAG).fetchSemanticsNode().boundsInRoot
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(
            bounds.left >= host.left && bounds.top >= host.top && bounds.right <= host.right &&
                bounds.bottom <= host.bottom,
            "$layout footer label must fit inside $host: $bounds",
        )
    }

    companion object {
        private const val HOST_TAG = "article-footer-host"
        private const val NEXT_SOURCE_TITLE = "Next source"
        private const val LONG_SOURCE_TITLE = "Next source with a long descriptive name for desktop readers"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun layouts(): List<Array<FeedLayout>> = listOf(FeedLayout.LIST, FeedLayout.GRID).map { arrayOf(it) }
    }
}
