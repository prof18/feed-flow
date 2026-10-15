package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
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
    fun `short choice dropdown exposes selected option and routes selection`() {
        val selected = mutableStateOf("first")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    SettingsChoice(
                        title = "Short choice",
                        choices = persistentListOf("first" to "First", "second" to "Second", "third" to "Third"),
                        selected = selected.value,
                        onSelected = { selected.value = it },
                    )
                }
            }
        }

        val trigger = rule.onNodeWithContentDescription("Short choice")
        trigger.assertIsEnabled()
        trigger.assertExists()
        trigger.performClick()
        selectedOption("First").assertIsSelected()
        rule.onNodeWithText("Second").performClick()
        rule.runOnIdle { assertEquals("second", selected.value) }
    }

    @Test
    fun `disabling an open choice dropdown blocks selection`() {
        val selected = mutableStateOf("first")
        val enabled = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    SettingsChoice(
                        title = "Disabled choice",
                        choices = persistentListOf("first" to "First", "second" to "Second"),
                        selected = selected.value,
                        onSelected = { selected.value = it },
                        enabled = enabled.value,
                    )
                }
            }
        }

        val trigger = rule.onNodeWithContentDescription("Disabled choice")
        trigger.performClick()
        val second = rule.onNodeWithText("Second")
        rule.runOnIdle { enabled.value = false }
        val disabledTrigger = rule.onNodeWithContentDescription("Disabled choice")
        disabledTrigger.assertIsNotEnabled()
        second.assertDoesNotExist()
        disabledTrigger.performClick()
        second.assertDoesNotExist()

        rule.runOnIdle { assertEquals("first", selected.value) }
    }

    @Test
    fun `long choice dropdown supports keyboard selection and escape cancellation`() {
        val selected = mutableStateOf("one")
        val choices = persistentListOf(
            "one" to "One",
            "two" to "Two",
            "three" to "Three",
            "four" to "Four",
            "five" to "Five",
            "six" to "Six",
            "seven" to "Seven",
            "eight" to "Eight",
        )
        rule.setContent {
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    SettingsChoice(
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
        comboBox.assertIsEnabled()
        comboBox.requestFocus()
        comboBox.performKeyInput { pressKey(Key.Spacebar) }
        selectedOption("One").assertIsSelected()
        comboBox.performKeyInput { pressKey(Key.DirectionDown) }
        rule.runOnIdle { assertEquals("one", selected.value) }
        comboBox.performKeyInput { pressKey(Key.MoveEnd) }
        rule.onNodeWithText("Eight").assertIsDisplayed()
        rule.runOnIdle { assertEquals("one", selected.value) }
        comboBox.performKeyInput { pressKey(Key.Escape) }
        rule.runOnIdle { assertEquals("one", selected.value) }

        comboBox.performKeyInput { pressKey(Key.Spacebar) }
        comboBox.performKeyInput { pressKey(Key.MoveEnd) }
        rule.runOnIdle { assertEquals("one", selected.value) }
        comboBox.performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals("eight", selected.value) }
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
    fun `cleanup confirmation rechecks availability after it opens`() {
        val enabled = mutableStateOf(true)
        var confirms = 0
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ConfirmationSetting(
                        title = EnFeedFlowStrings.clearOldArticlesButton,
                        dialogTitle = "Fixture cleanup",
                        dialogMessage = "Fixture cleanup confirmation",
                        onConfirm = { confirms++ },
                        enabled = enabled.value,
                    )
                }
            }
        }
        rule.onNodeWithText(EnFeedFlowStrings.clearOldArticlesButton).performClick()
        val confirm = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action
        rule.runOnIdle { enabled.value = false }
        rule.onNodeWithText(EnFeedFlowStrings.clearOldArticlesButton).assertIsNotEnabled()
        rule.runOnIdle { requireNotNull(confirm).invoke() }
        rule.runOnIdle { assertEquals(0, confirms) }
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).assertDoesNotExist()
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
                        onArticleOpenModeSelected = {},
                        onSaveReaderModeContentToggled = {},
                        onPrefetchToggled = { prefetchEnabled = it },
                        onMarkReadWhenScrollingToggled = {},
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
                    FeedVisibilitySettings(
                        isMarkReadWhenScrollingEnabled = true,
                        isShowReadItemsEnabled = true,
                        isHideReadItemsEnabled = false,
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

    private fun selectedOption(text: String) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.Selected, true) and
            androidx.compose.ui.test.hasText(text),
    )
}
