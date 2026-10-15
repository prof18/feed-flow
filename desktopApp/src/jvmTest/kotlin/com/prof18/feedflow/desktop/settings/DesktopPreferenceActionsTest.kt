package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class DesktopPreferenceActionsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `short choice exposes selected radio options and routes selection`() {
        val selected = mutableStateOf("first")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    JewelSettingsChoice(
                        title = "Short choice",
                        choices = persistentListOf("first" to "First", "second" to "Second", "third" to "Third"),
                        selected = selected.value,
                        onSelected = { selected.value = it },
                    )
                }
            }
        }

        val first = radioOption("First")
        first.assertIsSelected()
        first.performSemanticsAction(SemanticsActions.OnClick) { it() }

        rule.runOnIdle { assertEquals("first", selected.value) }
        radioOption("Second").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals("second", selected.value) }
    }

    @Test
    fun `disabled short choice rejects a captured stale action`() {
        val selected = mutableStateOf("first")
        val enabled = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    JewelSettingsChoice(
                        title = "Disabled choice",
                        choices = persistentListOf("first" to "First", "second" to "Second"),
                        selected = selected.value,
                        onSelected = { selected.value = it },
                        enabled = enabled.value,
                    )
                }
            }
        }

        val second = radioOption("Second")
        val staleAction = second.fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle { enabled.value = false }
        radioOption("Second").assertIsNotEnabled()
        staleAction?.invoke()

        rule.runOnIdle { assertEquals("first", selected.value) }
    }

    @Test
    fun `long choice has named combobox and routes chosen value`() {
        val selected = mutableStateOf("one")
        val choices = persistentListOf(
            "one" to "One",
            "two" to "Two",
            "three" to "Three",
            "four" to "Four",
            "five" to "Five",
        )
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    JewelSettingsChoice(
                        title = "Long choice",
                        choices = choices,
                        selected = selected.value,
                        onSelected = { selected.value = it },
                    )
                }
            }
        }

        val comboBox = rule.onNodeWithContentDescription("Long choice")
        comboBox.assertExists()
        comboBox.requestFocus()
        comboBox.performKeyInput { pressKey(Key.Spacebar) }
        comboBox.performKeyInput { pressKey(Key.MoveEnd) }
        comboBox.performKeyInput { pressKey(Key.Enter) }

        rule.runOnIdle { assertEquals("five", selected.value) }
        rule.onNodeWithText("Five").assertExists()
    }

    @Test
    fun `confirmation setting cancel dismisses and confirm runs once`() {
        var confirms = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ConfirmationSetting(
                        title = "Clear cache",
                        dialogTitle = "Confirm cache clear",
                        dialogMessage = "This cannot be undone.",
                        onConfirm = { confirms++ },
                    )
                }
            }
        }

        rule.onNodeWithText("Clear cache").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(0, confirms) }

        rule.onNodeWithText("Clear cache").performSemanticsAction(SemanticsActions.OnClick) { it() }
        val confirmAction = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle {
            requireNotNull(confirmAction).invoke()
            confirmAction.invoke()
        }
        rule.runOnIdle { assertEquals(1, confirms) }
    }

    @Test
    fun `reading pane prefetch cancel leaves preference unchanged and confirm enables it`() {
        var prefetchEnabled = false
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ReadingPane(
                        articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
                        isSaveReaderModeContentEnabled = false,
                        isPrefetchArticleContentEnabled = prefetchEnabled,
                        isMarkReadWhenScrollingEnabled = false,
                        isShowReadItemsEnabled = true,
                        isHideReadItemsEnabled = false,
                        onArticleOpenModeSelected = {},
                        onSaveReaderModeContentToggled = {},
                        onPrefetchToggled = { prefetchEnabled = it },
                        onMarkReadWhenScrollingToggled = {},
                        onShowReadItemsToggled = {},
                        onHideReadItemsToggled = {},
                    )
                }
            }
        }

        rule.onNodeWithText(EnFeedFlowStrings.settingsPrefetchArticleContent)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(false, prefetchEnabled) }

        rule.onNodeWithText(EnFeedFlowStrings.settingsPrefetchArticleContent)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(true, prefetchEnabled) }
    }

    @Test
    fun `hide read preference is disabled with explanation while marking read on scroll`() {
        var hideReadToggles = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ReadingPane(
                        articleOpenMode = ArticleOpenMode.FULL_ARTICLE,
                        isSaveReaderModeContentEnabled = false,
                        isPrefetchArticleContentEnabled = false,
                        isMarkReadWhenScrollingEnabled = true,
                        isShowReadItemsEnabled = true,
                        isHideReadItemsEnabled = false,
                        onArticleOpenModeSelected = {},
                        onSaveReaderModeContentToggled = {},
                        onPrefetchToggled = {},
                        onMarkReadWhenScrollingToggled = {},
                        onShowReadItemsToggled = {},
                        onHideReadItemsToggled = { hideReadToggles++ },
                    )
                }
            }
        }

        rule.onNodeWithText(EnFeedFlowStrings.settingsHideReadItems).assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.settingsHideReadItemsDisabledWhileScrolling).assertExists()
        rule.runOnIdle { assertEquals(0, hideReadToggles) }
    }

    private fun radioOption(text: String) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton) and
            androidx.compose.ui.test.hasText(text),
    )
}
