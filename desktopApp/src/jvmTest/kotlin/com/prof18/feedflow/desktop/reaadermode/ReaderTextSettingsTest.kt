package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ReaderModeDefaults
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderTextSettingsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `font control receives initial focus and arrow keys change the current size`() {
        val fontSize = mutableIntStateOf(ReaderModeDefaults.FONT_SIZE)
        val lineHeight = mutableIntStateOf(ReaderModeDefaults.LINE_HEIGHT)
        showSettings(fontSize, lineHeight)

        val fontControl = rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeFontSize)
        fontControl.assertIsFocused()
        fontControl.performKeyInput { pressKey(Key.DirectionRight) }

        rule.runOnIdle { assertEquals(ReaderModeDefaults.FONT_SIZE + 1, fontSize.intValue) }
    }

    @Test
    fun `named line spacing control updates its current value`() {
        val fontSize = mutableIntStateOf(ReaderModeDefaults.FONT_SIZE)
        val lineHeight = mutableIntStateOf(ReaderModeDefaults.LINE_HEIGHT)
        showSettings(fontSize, lineHeight)

        rule.onNodeWithContentDescription(EnFeedFlowStrings.readerModeLineHeight)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(NEW_LINE_HEIGHT.toFloat()) }

        rule.runOnIdle { assertEquals(NEW_LINE_HEIGHT, lineHeight.intValue) }
        rule.onNodeWithText("2.0").assertExists()
    }

    @Test
    fun `reset restores both live preferences and disables when already at defaults`() {
        val fontSize = mutableIntStateOf(NEW_FONT_SIZE)
        val lineHeight = mutableIntStateOf(NEW_LINE_HEIGHT)
        showSettings(fontSize, lineHeight)

        rule.onNodeWithText(EnFeedFlowStrings.readerModeResetToDefault).performClick()

        rule.runOnIdle {
            assertEquals(ReaderModeDefaults.FONT_SIZE, fontSize.intValue)
            assertEquals(ReaderModeDefaults.LINE_HEIGHT, lineHeight.intValue)
        }
        rule.onNodeWithText(EnFeedFlowStrings.readerModeResetToDefault).assertIsNotEnabled()
    }

    @Test
    fun `Done dismisses without reverting live preference changes`() {
        val fontSize = mutableIntStateOf(NEW_FONT_SIZE)
        val lineHeight = mutableIntStateOf(NEW_LINE_HEIGHT)
        var dismissals = 0
        showSettings(fontSize, lineHeight, onDismiss = { dismissals++ })

        rule.onNodeWithText(EnFeedFlowStrings.actionDone).performClick()

        rule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(NEW_FONT_SIZE, fontSize.intValue)
            assertEquals(NEW_LINE_HEIGHT, lineHeight.intValue)
        }
    }

    @Test
    fun `large RTL text keeps both footer actions within the compact content width`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    Box(Modifier.size(width = 360.dp, height = 520.dp).testTag(COMPACT_TAG)) {
                        ReaderTextSettingsContent(
                            fontSize = NEW_FONT_SIZE,
                            lineHeight = NEW_LINE_HEIGHT,
                            onFontSizeChange = {},
                            onLineHeightChange = {},
                            onDismiss = {},
                        )
                    }
                }
            }
        }
        val content = rule.onNodeWithTag(COMPACT_TAG).fetchSemanticsNode().boundsInRoot
        listOf(EnFeedFlowStrings.readerModeResetToDefault, EnFeedFlowStrings.actionDone).forEach { label ->
            val bounds = rule.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= content.left && bounds.right <= content.right, "$label: $bounds in $content")
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertTrue(
                !layout.hasVisualOverflow,
                "$label must remain fully readable: ${layout.size}, ${layout.layoutInput.constraints}, " +
                    "lines=${layout.lineCount}, overflowWidth=${layout.didOverflowWidth}, " +
                    "overflowHeight=${layout.didOverflowHeight}",
            )
        }
    }

    private fun showSettings(
        fontSize: androidx.compose.runtime.MutableIntState,
        lineHeight: androidx.compose.runtime.MutableIntState,
        onDismiss: () -> Unit = {},
    ) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ReaderTextSettingsContent(
                        fontSize = fontSize.intValue,
                        lineHeight = lineHeight.intValue,
                        onFontSizeChange = { fontSize.intValue = it },
                        onLineHeightChange = { lineHeight.intValue = it },
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    private companion object {
        const val NEW_FONT_SIZE = 22
        const val NEW_LINE_HEIGHT = 5
        const val COMPACT_TAG = "reader-settings-compact"
    }
}
