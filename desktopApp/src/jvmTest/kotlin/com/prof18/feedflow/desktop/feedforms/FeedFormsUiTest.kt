package com.prof18.feedflow.desktop.feedforms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedFormsUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `invalid add error requires acknowledgement before force retry`() {
        var forced = 0
        val loading = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AddFeedForm(
                        feedUrl = "https://example.test/feed.xml",
                        showError = true,
                        showLoading = loading.value,
                        errorMessage = EnFeedFlowStrings.invalidRssUrlWithRetryHint,
                        categoriesState = CategoriesState(),
                        showNotificationToggle = false,
                        isNotificationEnabled = false,
                        canForceAdd = true,
                        onFeedUrlUpdated = {},
                        onAddFeed = {},
                        categoryActions = TestCategoryActions,
                        onNotificationToggleChanged = {},
                        onForceAddFeed = { forced++ },
                    )
                }
            }
        }

        rule.onNodeWithTag(ForceAddFeedTag).assertIsNotEnabled()
        rule.onNodeWithText(EnFeedFlowStrings.addFeedAnywayAcknowledgement).performClick()
        rule.onNodeWithTag(ForceAddFeedTag).performClick()
        rule.runOnIdle { assertEquals(1, forced) }

        rule.runOnIdle { loading.value = true }
        rule.onNodeWithTag(ForceAddFeedTag).assertIsNotEnabled()
    }

    @Test
    fun `parent echoed URL updates preserve typing and caret`() {
        var feedUrl by mutableStateOf("")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    AddFeedForm(
                        feedUrl = feedUrl,
                        showError = false,
                        showLoading = false,
                        errorMessage = "",
                        categoriesState = CategoriesState(),
                        showNotificationToggle = false,
                        isNotificationEnabled = false,
                        canForceAdd = false,
                        onFeedUrlUpdated = { feedUrl = it },
                        onAddFeed = {},
                        categoryActions = TestCategoryActions,
                        onNotificationToggleChanged = {},
                        onForceAddFeed = {},
                    )
                }
            }
        }

        val urlField = rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
        urlField.performTextInput("a")
        rule.waitForIdle()
        urlField.performTextInput("b")
        rule.waitForIdle()

        assertFieldTextAndCursor(FeedFormE2eIds.ADD_FEED_URL_INPUT, "ab", 2)
        rule.runOnIdle { assertEquals("ab", feedUrl) }
    }

    @Test
    fun `parent echoed edit name and URL updates preserve successive typing and caret`() {
        var feedName by mutableStateOf("")
        var feedUrl by mutableStateOf("")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    EditFeedForm(
                        feedUrl = feedUrl,
                        feedName = feedName,
                        showError = false,
                        showLoading = false,
                        errorMessage = "",
                        canEditUrl = true,
                        categoriesState = CategoriesState(),
                        feedSourceSettings = com.prof18.feedflow.core.model.FeedSourceSettings(),
                        onFeedUrlUpdated = { feedUrl = it },
                        onFeedNameUpdated = { feedName = it },
                        articleOpenModeSelector = { _, _ -> },
                        onHiddenToggled = {},
                        onHideImagesToggled = {},
                        onPinnedToggled = {},
                        showNotificationToggle = false,
                        onNotificationToggleChanged = {},
                        categoryActions = TestCategoryActions,
                        editFeed = {},
                        onShowDeleteDialog = {},
                        showDeleteDialog = false,
                        onDismissDeleteDialog = {},
                        onConfirmDelete = {},
                    )
                }
            }
        }

        val nameField = rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT)
        nameField.performTextInput("a")
        rule.waitForIdle()
        nameField.performTextInput("b")
        rule.waitForIdle()
        assertFieldTextAndCursor(FeedFormE2eIds.NAME_INPUT, "ab", 2)

        val urlField = rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
        urlField.performTextInput("a")
        rule.waitForIdle()
        urlField.performTextInput("b")
        rule.waitForIdle()
        assertFieldTextAndCursor(FeedFormE2eIds.ADD_FEED_URL_INPUT, "ab", 2)
        rule.runOnIdle {
            assertEquals("ab", feedName)
            assertEquals("ab", feedUrl)
        }
    }

    @Test
    fun `external edit feed replacement updates name and URL without stale text`() {
        var feedName by mutableStateOf("Old name")
        var feedUrl by mutableStateOf("https://old.example/feed.xml")
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    EditFeedForm(
                        feedUrl = feedUrl,
                        feedName = feedName,
                        showError = false,
                        showLoading = false,
                        errorMessage = "",
                        canEditUrl = true,
                        categoriesState = CategoriesState(),
                        feedSourceSettings = com.prof18.feedflow.core.model.FeedSourceSettings(),
                        onFeedUrlUpdated = { feedUrl = it },
                        onFeedNameUpdated = { feedName = it },
                        articleOpenModeSelector = { _, _ -> },
                        onHiddenToggled = {},
                        onHideImagesToggled = {},
                        onPinnedToggled = {},
                        showNotificationToggle = false,
                        onNotificationToggleChanged = {},
                        categoryActions = TestCategoryActions,
                        editFeed = {},
                        onShowDeleteDialog = {},
                        showDeleteDialog = false,
                        onDismissDeleteDialog = {},
                        onConfirmDelete = {},
                    )
                }
            }
        }

        rule.runOnIdle {
            feedName = "Replacement feed"
            feedUrl = "https://replacement.example/rss.xml"
        }

        rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT).assertTextEquals("Replacement feed")
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
            .assertTextEquals("https://replacement.example/rss.xml")
    }

    @Test
    fun `confirmation cancel routes dismiss without confirming`() {
        var confirmed = false
        var dismissed = false
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ConfirmationDialog(
                        title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                        message = EnFeedFlowStrings.deleteFeedConfirmationMessage,
                        onConfirm = { confirmed = true },
                        onDismiss = { dismissed = true },
                    )
                }
            }
        }

        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertTrue(dismissed)
            assertEquals(false, confirmed)
        }
    }

    private fun assertFieldTextAndCursor(tag: String, expectedText: String, cursor: Int) {
        val field = rule.onNodeWithTag(tag)
        field.assertTextEquals(expectedText)
        val selection = field.fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
        assertEquals(TextRange(cursor), selection)
    }
}

private val TestCategoryActions = FeedCategoryActions(
    onSelect = {},
    onAdd = {},
    validate = { _, _ -> com.prof18.feedflow.core.model.CategoryNameValidationResult.VALID },
)
