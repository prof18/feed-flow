package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.ui.DesktopFeedbackDuration
import com.prof18.feedflow.desktop.ui.DesktopFeedbackState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class FeedbackLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `compact actionable feedback keeps message and controls readable`() {
        val state = DesktopFeedbackState()
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val message = "Feed refresh failed for this source. Check the connection and try again. " +
            "Your saved articles are unchanged."
        rule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction.value,
                LocalDensity provides Density(currentDensity.density, fontScale.value),
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    LaunchedEffect(state) {
                        state.showFeedback(
                            message = message,
                            actionLabel = EnFeedFlowStrings.retryButton,
                            duration = DesktopFeedbackDuration.Indefinite,
                        )
                    }
                    Box(Modifier.size(width = 360.dp, height = 500.dp).testTag(CONTENT_TAG)) {
                        SnackbarHost(state)
                    }
                }
            }
        }

        for (scale in FONT_SCALES) {
            for (layoutDirection in LayoutDirection.entries) {
                rule.runOnIdle {
                    fontScale.value = scale
                    direction.value = layoutDirection
                }
                for (mode in THEMES) {
                    rule.runOnIdle { theme.value = mode }
                    val bounds = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
                    assertReadable(message, bounds)
                    assertReadable(EnFeedFlowStrings.retryButton, bounds)
                    assertReadable(EnFeedFlowStrings.closeButtonContentDescription, bounds)
                }
            }
        }
    }

    @Test
    fun `normal feedback keeps localized message and close label readable`() {
        val state = DesktopFeedbackState()
        val theme = mutableStateOf(ThemeMode.LIGHT)
        val fontScale = mutableStateOf(1f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        val message = EnFeedFlowStrings.feedAddedMessageWithoutName
        rule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction.value,
                LocalDensity provides Density(currentDensity.density, fontScale.value),
            ) {
                FeedFlowTheme(theme.value != ThemeMode.LIGHT, theme.value == ThemeMode.OLED) {
                    LaunchedEffect(state) {
                        state.showFeedback(message, duration = DesktopFeedbackDuration.Indefinite)
                    }
                    Box(Modifier.size(width = 500.dp, height = 220.dp).testTag(CONTENT_TAG)) {
                        SnackbarHost(state)
                    }
                }
            }
        }

        for (scale in FONT_SCALES) {
            for (layoutDirection in LayoutDirection.entries) {
                rule.runOnIdle {
                    fontScale.value = scale
                    direction.value = layoutDirection
                }
                for (mode in THEMES) {
                    rule.runOnIdle { theme.value = mode }
                    val bounds = rule.onNodeWithTag(CONTENT_TAG).fetchSemanticsNode().boundsInRoot
                    assertReadable(message, bounds)
                    assertReadable(EnFeedFlowStrings.closeButtonContentDescription, bounds)
                }
            }
        }
    }

    private fun assertReadable(text: String, content: Rect) {
        val node = rule.onNodeWithText(text)
        node.assertIsDisplayed()
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(
            bounds.left >= content.left && bounds.right <= content.right &&
                bounds.top >= content.top && bounds.bottom <= content.bottom,
            "$text: $bounds in $content",
        )
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(!layouts.single().hasVisualOverflow, "$text must be fully readable: ${layouts.single().size}")
    }

    private companion object {
        const val CONTENT_TAG = "feedback-content-fixture"
        val FONT_SCALES = listOf(1f, 2f)
        val THEMES = listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.OLED)
    }
}
