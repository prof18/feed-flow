package com.prof18.feedflow.desktop.feedforms

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleOpenMode
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.ArticleOpenModeSelector
import com.prof18.feedflow.desktop.ui.components.ConfirmationDialog
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedSubmissionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `add blocks a captured second activation before loading arrives`() {
        var submitted = 0
        content { AddForm(onAdd = { submitted++ }) }
        activateTwice(EnFeedFlowStrings.addFeed)
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `add URL receives initial focus and Enter submits once`() {
        var submitted = 0
        content { AddForm(onAdd = { submitted++ }) }
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT).assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `edit blocks a captured second activation including unchanged save`() {
        var submitted = 0
        content { EditForm(onSave = { submitted++ }) }
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `edit name receives initial focus and Enter saves`() {
        var submitted = 0
        content { EditForm(onSave = { submitted++ }) }
        rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT).assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `edit cannot submit an empty name even through a captured action`() {
        var submitted = 0
        content { EditForm(name = "", onSave = { submitted++ }) }
        rule.onNodeWithText(EnFeedFlowStrings.actionSave).assertIsNotEnabled()
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { assertEquals(0, submitted) }
    }

    @Test
    fun `a completed loading cycle allows a retry`() {
        var submitted = 0
        val loading = mutableStateOf(false)
        val revision = mutableIntStateOf(0)
        content { AddForm(loading = loading.value, revision = revision.value, onAdd = { submitted++ }) }
        activateTwice(EnFeedFlowStrings.addFeed)
        rule.runOnIdle { loading.value = true }
        rule.waitForIdle()
        rule.runOnIdle {
            loading.value = false
            revision.value++
        }
        rule.waitForIdle()
        activateTwice(EnFeedFlowStrings.addFeed)
        rule.runOnIdle { assertEquals(2, submitted) }
    }

    @Test
    fun `nonterminal loading to idle transition cannot release an in flight edit`() {
        var submitted = 0
        val loading = mutableStateOf(false)
        content { EditForm(loading = loading.value, onSave = { submitted++ }) }
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { loading.value = true }
        rule.waitForIdle()
        rule.runOnIdle { loading.value = false }
        rule.onNodeWithText(EnFeedFlowStrings.actionSave).assertIsNotEnabled()
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `article opening selector cannot change the draft while saving`() {
        var modeChanges = 0
        content { EditForm(onModeSelected = { modeChanges++ }, onSave = {}) }
        val dropdown = rule.onNodeWithContentDescription(EnFeedFlowStrings.articleOpenMode)
        dropdown.performSemanticsAction(SemanticsActions.OnClick) { it() }
        val option = rule.onNodeWithText(EnFeedFlowStrings.readerContentSourceWeb)
        option.assertIsEnabled()
        val select = option.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        activateTwice(EnFeedFlowStrings.actionSave)
        dropdown.assertIsNotEnabled()
        option.assertDoesNotExist()
        rule.runOnIdle {
            select()
            assertEquals(0, modeChanges)
        }
    }

    @Test
    fun `confirmation dispatches and dismisses only once before composition removes it`() {
        var confirmed = 0
        var dismissed = 0
        content {
            ConfirmationDialog(
                title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                message = EnFeedFlowStrings.deleteFeedConfirmationMessage,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
        activateTwice(EnFeedFlowStrings.confirmButton)
        rule.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun `each fast failure unlocks a retry without losing the typed URL`() {
        var submitted = 0
        val revision = mutableIntStateOf(0)
        val url = mutableStateOf("https://example.test/feed.xml")
        content {
            AddForm(
                url = url.value,
                onUrlUpdated = { url.value = it },
                revision = revision.value,
                error = "Same failure",
                onAdd = { submitted++ },
            )
        }
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
            .performTextReplacement("https://example.test/new.xml")
        repeat(2) {
            activateTwice(EnFeedFlowStrings.addFeed)
            rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsNotEnabled()
            rule.runOnIdle { revision.value++ }
            rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsEnabled()
            rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
                .assertTextEquals("https://example.test/new.xml").assertIsFocused()
        }
        rule.runOnIdle { assertEquals(2, submitted) }
    }

    @Test
    fun `clearing an old error does not release a pending add`() {
        var submitted = 0
        val error = mutableStateOf("Old failure")
        content { AddForm(error = error.value, onAdd = { submitted++ }) }
        activateTwice(EnFeedFlowStrings.addFeed)
        rule.runOnIdle { error.value = "" }
        rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsNotEnabled()
        activateTwice(EnFeedFlowStrings.addFeed)
        rule.runOnIdle { assertEquals(1, submitted) }
    }

    @Test
    fun `force add requires acknowledgement for Enter and dispatches once when acknowledged`() {
        var added = 0
        var forced = 0
        content { AddForm(force = true, error = "Failure", onAdd = { added++ }, onForce = { forced++ }) }
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT).performKeyInput { pressKey(Key.Enter) }
        rule.runOnIdle { assertEquals(0, forced) }
        rule.onNodeWithText(EnFeedFlowStrings.addFeedAnywayAcknowledgement).performClick()
        activateTwice(EnFeedFlowStrings.addFeedAnywayButton)
        rule.runOnIdle {
            assertEquals(1, forced)
            assertEquals(0, added)
        }
    }

    @Test
    fun `fast edit failure permits retry and retains the name`() {
        var submitted = 0
        val revision = mutableIntStateOf(0)
        val name = mutableStateOf("Example")
        content {
            EditForm(name.value, revision.value, { name.value = it }, onSave = { submitted++ })
        }
        rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT).performTextReplacement("Revised name")
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { revision.value++ }
        rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT).assertTextEquals("Revised name").assertIsFocused()
        activateTwice(EnFeedFlowStrings.actionSave)
        rule.runOnIdle { assertEquals(2, submitted) }
    }

    @Test
    fun `feed delete blocks a captured second confirmation`() {
        var deleted = 0
        content { DeleteFeedConfirmation(true, {}, { deleted++ }) }
        activateTwice(EnFeedFlowStrings.deleteFeed)
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    @Test
    fun `add URL has a persistent visible and accessible field label`() {
        content { AddForm(onAdd = {}) }
        rule.onNodeWithText(EnFeedFlowStrings.feedUrl).assertIsDisplayed()
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
            .assertContentDescriptionEquals(EnFeedFlowStrings.feedUrl)
    }

    @Test
    fun `edit name and URL have persistent accessible field labels`() {
        content { EditForm(onSave = {}) }
        rule.onNodeWithText(EnFeedFlowStrings.feedName).assertIsDisplayed()
        rule.onNodeWithText(EnFeedFlowStrings.feedUrl).assertIsDisplayed()
        rule.onNodeWithTag(FeedFormE2eIds.NAME_INPUT).assertContentDescriptionEquals(EnFeedFlowStrings.feedName)
        rule.onNodeWithTag(FeedFormE2eIds.ADD_FEED_URL_INPUT)
            .assertContentDescriptionEquals(EnFeedFlowStrings.feedUrl)
    }

    @Test
    fun `edit submission error appears once`() {
        content { EditForm(error = "Failed to save fixture", onSave = {}) }
        rule.onAllNodesWithText("Failed to save fixture").assertCountEquals(1)
    }

    @Test
    fun `compact enlarged RTL edit keeps save and delete visible with readable text`() {
        compactContent(LayoutDirection.Rtl) { EditForm(onSave = {}) }
        assertReadableAction(EnFeedFlowStrings.actionSave)
        assertReadableAction(EnFeedFlowStrings.deleteFeedButton)
    }

    @Test
    fun `compact enlarged add keeps its action visible below a long scrolling body`() {
        compactContent(LayoutDirection.Ltr) {
            AddForm(status = "Fixture status paragraph. ".repeat(n = 40), onAdd = {})
        }
        assertReadableAction(EnFeedFlowStrings.addFeed)
    }

    private fun assertReadableAction(label: String) {
        rule.onNodeWithText(label).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(!layouts.single().hasVisualOverflow, "$label must remain fully readable")
    }

    private fun compactContent(direction: LayoutDirection, body: @Composable () -> Unit) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalFeedFlowStrings provides EnFeedFlowStrings,
                LocalLayoutDirection provides direction,
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                FeedFlowTheme(darkTheme = true, useOledTheme = true) {
                    Box(Modifier.size(width = 360.dp, height = 500.dp)) { body() }
                }
            }
        }
    }

    private fun activateTwice(label: String) {
        val activate = rule.onNodeWithText(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            activate()
            activate()
        }
    }

    private fun content(body: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    @Composable
    private fun AddForm(
        loading: Boolean = false,
        revision: Int = 0,
        error: String = "",
        force: Boolean = false,
        url: String = "https://example.test/feed.xml",
        onUrlUpdated: (String) -> Unit = {},
        onForce: () -> Unit = {},
        status: String? = null,
        onAdd: () -> Unit,
    ) {
        AddFeedForm(
            feedUrl = url,
            showError = error.isNotEmpty(),
            showLoading = loading,
            errorMessage = error,
            categoriesState = CategoriesState(),
            showNotificationToggle = false,
            isNotificationEnabled = false,
            canForceAdd = force,
            onFeedUrlUpdated = onUrlUpdated,
            onAddFeed = onAdd,
            categoryActions = TestCategoryActions,
            onNotificationToggleChanged = {},
            onForceAddFeed = onForce,
            completionRevision = revision,
            statusMessage = status,
        )
    }

    @Composable
    private fun EditForm(
        name: String = "Example",
        revision: Int = 0,
        onNameUpdated: (String) -> Unit = {},
        loading: Boolean = false,
        onModeSelected: (ArticleOpenMode) -> Unit = {},
        error: String = "",
        onSave: () -> Unit,
    ) {
        EditFeedForm(
            feedUrl = "https://example.test/feed.xml",
            feedName = name,
            showError = error.isNotEmpty(),
            showLoading = loading,
            errorMessage = error,
            canEditUrl = true,
            categoriesState = CategoriesState(),
            feedSourceSettings = FeedSourceSettings(),
            onFeedUrlUpdated = {},
            onFeedNameUpdated = onNameUpdated,
            articleOpenModeSelector = { modifier, enabled ->
                ArticleOpenModeSelector(
                    ArticleOpenMode.DEFAULT,
                    onModeSelected,
                    modifier,
                    allowDefault = true,
                    enabled = enabled,
                )
            },
            onHiddenToggled = {},
            onHideImagesToggled = {},
            onPinnedToggled = {},
            showNotificationToggle = false,
            onNotificationToggleChanged = {},
            categoryActions = TestCategoryActions,
            editFeed = onSave,
            onShowDeleteDialog = {},
            showDeleteDialog = false,
            onDismissDeleteDialog = {},
            onConfirmDelete = {},
            completionRevision = revision,
        )
    }
}

private val TestCategoryActions = FeedCategoryActions(
    onSelect = {},
    onAdd = {},
    validate = { _, _ -> com.prof18.feedflow.core.model.CategoryNameValidationResult.VALID },
)
