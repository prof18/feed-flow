package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfirmationLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `large RTL confirmation labels remain readable in every theme at compact width`() {
        val theme = mutableStateOf(ThemeMode.LIGHT)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    Box(Modifier.size(width = 360.dp, height = 300.dp).testTag(CONTENT_TAG)) {
                        ConfirmationContent(
                            title = EnFeedFlowStrings.deleteAllFeedsConfirmationTitle,
                            message = EnFeedFlowStrings.deleteAllFeedsConfirmationMessage,
                            onConfirm = {},
                            onDismiss = {},
                            modifier = Modifier.fillMaxSize(),
                            confirmLabel = EnFeedFlowStrings.deleteAllFeedsInCategory,
                        )
                    }
                }
            }
        }
        listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED).forEach { mode ->
            rule.runOnIdle { theme.value = mode }
            val bounds = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
            assertReadable(EnFeedFlowStrings.cancelButton, bounds)
            assertReadable(EnFeedFlowStrings.deleteAllFeedsInCategory, bounds)
        }
    }

    @Test
    fun `long confirmation message scrolls while both actions stay visible`() {
        val message = (1..MESSAGE_PARAGRAPHS).joinToString("\n\n") { "Confirmation fixture paragraph $it" }
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = true, useOledTheme = false) {
                    Box(Modifier.size(width = 360.dp, height = 260.dp).testTag(CONTENT_TAG)) {
                        ConfirmationContent(
                            title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                            message = message,
                            onConfirm = {},
                            onDismiss = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        val bounds = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
        assertReadable(EnFeedFlowStrings.cancelButton, bounds)
        assertReadable(EnFeedFlowStrings.confirmButton, bounds)
        val cancelBefore = rule.onNodeWithText(EnFeedFlowStrings.cancelButton).fetchSemanticsNode().boundsInRoot
        val confirmBefore = rule.onNodeWithText(EnFeedFlowStrings.confirmButton).fetchSemanticsNode().boundsInRoot

        val messageScroll = rule.onNode(hasScrollAction())
        messageScroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLL_DISTANCE) }
        val range = messageScroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(range.value() > 0f)
        assertEquals(range.maxValue(), range.value())
        rule.onNodeWithText(message).assertExists()
        assertEquals(
            cancelBefore,
            rule.onNodeWithText(EnFeedFlowStrings.cancelButton).fetchSemanticsNode().boundsInRoot,
        )
        assertEquals(
            confirmBefore,
            rule.onNodeWithText(EnFeedFlowStrings.confirmButton).fetchSemanticsNode().boundsInRoot,
        )
    }

    private fun assertReadable(label: String, content: Rect) {
        val node = rule.onNodeWithText(label)
        node.assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(
            bounds.left >= content.left && bounds.right <= content.right &&
                bounds.top >= content.top && bounds.bottom <= content.bottom,
            "$label: $bounds in $content",
        )
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertTrue(!layout.hasVisualOverflow, "$label must be fully readable: ${layout.size}")
    }

    private companion object {
        const val CONTENT_TAG = "confirmation-content-fixture"
        const val MESSAGE_PARAGRAPHS = 20
        const val SCROLL_DISTANCE = 10_000f
    }
}
