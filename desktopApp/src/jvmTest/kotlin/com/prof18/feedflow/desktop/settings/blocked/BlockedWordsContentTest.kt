@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.settings.blocked

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.desktop.settings.DesktopSettingsDialog
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlockedWordsContentTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `input receives initial focus with a persistent accessible label`() {
        content { Words() }
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT)
            .assertIsFocused()
            .assertContentDescriptionEquals(EnFeedFlowStrings.addWordPlaceholder)
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performTextReplacement("fixture")
        rule.onAllNodesWithText(EnFeedFlowStrings.addWordPlaceholder, useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun `empty and whitespace drafts leave a visible disabled add action`() {
        var additions = 0
        content { Words(onAdd = { additions++ }) }
        rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON).assertIsDisplayed().assertIsNotEnabled()
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performTextReplacement("   ")
        rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON).assertIsNotEnabled()
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(0, additions) }
    }

    @Test
    fun `Enter adds the trimmed word and clears the focused draft`() {
        val additions = mutableListOf<String>()
        content { Words(onAdd = additions::add) }
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).requestFocus().performTextReplacement("  fixture  ")
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(listOf("fixture"), additions) }
        assertEmptyFocusedDraft()
    }

    @Test
    fun `click adds once and restores focus even when a second captured click runs`() {
        val additions = mutableListOf<String>()
        content { Words(onAdd = additions::add) }
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).performTextReplacement("  fixture  ")
        val add = rule.onNodeWithTag(BlockedWordsE2eIds.ADD_BUTTON)
        val activate = add.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        add.requestFocus()
        rule.runOnIdle {
            activate()
            activate()
            assertEquals(listOf("fixture"), additions)
        }
        assertEmptyFocusedDraft()
    }

    @Test
    fun `named delete removes only its word and restores input focus`() {
        val words = mutableStateOf(persistentListOf("fixture", "keep"))
        content {
            Words(words = words.value, onRemove = { words.value = words.value.remove(it) })
        }
        rule.onNodeWithTag(BlockedWordsE2eIds.deleteButton("fixture"))
            .assertContentDescriptionEquals(EnFeedFlowStrings.deleteFeed)
            .requestFocus().performClick()
        rule.onNodeWithTag(BlockedWordsE2eIds.row("fixture")).assertDoesNotExist()
        rule.onNodeWithTag(BlockedWordsE2eIds.row("keep")).assertIsDisplayed()
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).assertIsFocused()
    }

    @Test
    fun `long word stays one line while its named delete action remains available`() {
        val word = "A long fixture phrase ".repeat(n = 30)
        content { Box(Modifier.size(width = 360.dp, height = 500.dp)) { Words(persistentListOf(word)) } }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(word).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(1, layouts.single().lineCount)
        rule.onNodeWithTag(BlockedWordsE2eIds.deleteButton(word))
            .assertContentDescriptionEquals(EnFeedFlowStrings.deleteFeed).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `large RTL text keeps Done readable below a scrolling word list`() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    Box(Modifier.size(width = 360.dp, height = 500.dp)) {
                        Words(List(size = 40) { "fixture $it" }.toImmutableList())
                    }
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(EnFeedFlowStrings.actionDone)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(!layouts.single().hasVisualOverflow)
    }

    @Test
    fun `Done dismisses without any word mutation`() {
        var dismissals = 0
        var mutations = 0
        content { Words(onAdd = { mutations++ }, onRemove = { mutations++ }, onClose = { dismissals++ }) }
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).performClick()
        rule.runOnIdle {
            assertEquals(1, dismissals)
            assertEquals(0, mutations)
        }
    }

    @Test
    fun `native Escape dismisses the blocked words window without adding its draft`() {
        val visible = mutableStateOf(true)
        var mutations = 0
        content {
            if (visible.value) {
                DesktopSettingsDialog(onCloseRequest = { visible.value = false }) {
                    Words(onAdd = { mutations++ })
                }
            }
        }
        rule.waitUntilExactlyOneExists(
            androidx.compose.ui.test.hasTestTag(BlockedWordsE2eIds.INPUT),
            timeoutMillis = 5_000,
        )
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT).requestFocus().performTextReplacement("unsaved fixture")
        SwingUtilities.invokeAndWait {
            val window = Window.getWindows().filterIsInstance<Dialog>().single {
                it.isVisible && it.title == EnFeedFlowStrings.settingsTitle
            }
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val event = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(event) }
        }
        rule.waitUntil(timeoutMillis = 5_000) { !visible.value }
        rule.runOnIdle { assertEquals(0, mutations) }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun assertEmptyFocusedDraft() {
        rule.onNodeWithTag(BlockedWordsE2eIds.INPUT)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
            .assertIsFocused()
    }

    private fun content(body: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    @Composable
    private fun Words(
        words: ImmutableList<String> = persistentListOf(),
        onAdd: (String) -> Unit = {},
        onRemove: (String) -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        BlockedWordsContent(words, onAdd, onRemove, onClose)
    }
}
