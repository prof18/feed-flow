package com.prof18.feedflow.desktop.feedforms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.CategoryId
import com.prof18.feedflow.core.model.CategoryName
import com.prof18.feedflow.core.model.CategoryNameValidationResult
import com.prof18.feedflow.desktop.home.drawer.DrawerDeleteAllFeedsDialog
import com.prof18.feedflow.desktop.home.drawer.DrawerDeleteCategoryDialog
import com.prof18.feedflow.desktop.testing.FeedFormE2eIds
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CategoryDialogTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `category actions leave room for a readable Pagination label at default window size`() {
        content { CategoryManager("Pagination") }
        val layouts = categoryLabelLayouts("Pagination")
        assertEquals(1, layouts.single().lineCount)
        assertTrue(!layouts.single().hasVisualOverflow)
    }

    @Test
    fun `long category label is one line while edit and delete remain named and available`() {
        val name = "A long fixture category name ".repeat(n = 20)
        content { CategoryManager(name) }
        assertEquals(1, categoryLabelLayouts(name).single().lineCount)
        rule.onNodeWithTag("edit_feed_category_edit_fixture")
            .assertContentDescriptionEquals(EnFeedFlowStrings.editCategory)
            .assertIsEnabled()
        rule.onNodeWithTag("edit_feed_category_delete_fixture")
            .assertContentDescriptionEquals(EnFeedFlowStrings.deleteCategory)
            .assertIsEnabled()
        rule.onNodeWithTag("edit_feed_category_edit_fixture").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(FeedFormE2eIds.CATEGORY_RENAME_INPUT).fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_RENAME_INPUT).assertTextEquals(name)
    }

    private fun categoryLabelLayouts(name: String): List<TextLayoutResult> {
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(name, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts
    }

    @Composable
    private fun CategoryManager(name: String) {
        CategoryEditorDialog(
            categoryState = CategoriesState(persistentListOf(CategoriesState.CategoryItem("fixture", name, false))),
            onCategorySelected = {},
            onAddCategory = {},
            onDeleteCategory = {},
            onEditCategory = { _, _ -> },
            validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
            onDismiss = {},
        )
    }

    @Test
    fun `category name receives focus and Enter saves the trimmed validated name`() {
        var name = ""
        content { NameDialog(onSave = { name = it.name }) }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).assertIsFocused()
            .performTextReplacement("  News  ")
        rule.waitUntil(timeoutMillis = 5_000) {
            runCatching { rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_CONFIRM).assertIsEnabled() }.isSuccess
        }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).performKeyInput { pressKey(Key.Enter) }
        rule.waitUntil(timeoutMillis = 5_000) { name == "News" }
        rule.runOnIdle { assertEquals("News", name) }
    }

    @Test
    fun `a captured second category save cannot submit twice`() {
        var saves = 0
        content { NameDialog(initial = "News", onSave = { saves++ }) }
        activateTwice(EnFeedFlowStrings.confirmButton)
        rule.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun `an external rename echo for the same category cannot overwrite the draft`() {
        val initial = mutableStateOf("Old name")
        content { NameDialog(initial = initial.value, title = initial.value, categoryId = CategoryId("news")) }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_RENAME_INPUT).performTextReplacement("My draft")
        rule.runOnIdle { initial.value = "External update" }
        rule.waitUntil(timeoutMillis = 5_000) {
            var updated = false
            SwingUtilities.invokeAndWait {
                updated = Window.getWindows().filterIsInstance<Dialog>().any {
                    it.isVisible && it.title == "External update"
                }
            }
            updated
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(EnFeedFlowStrings.categoryNameAlreadyExists).fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_RENAME_INPUT).assertTextEquals("My draft")
    }

    @Test
    fun `cancelled category dialog cannot confirm through a captured action`() {
        var saves = 0
        var dismissals = 0
        content { NameDialog(initial = "News", onSave = { saves++ }, onDismiss = { dismissals++ }) }
        val confirm = rule.onNodeWithText(EnFeedFlowStrings.confirmButton)
            .fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            confirm()
            assertEquals(0, saves)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun `disabled category Enter cannot mutate`() {
        var saves = 0
        content { NameDialog(initial = "News", enabled = false, onSave = { saves++ }) }
        activateTwice(EnFeedFlowStrings.confirmButton)
        rule.runOnIdle { assertEquals(0, saves) }
    }

    @Test
    fun `category selection uses Done because selection changes are already applied`() {
        var mutations = 0
        var dismissals = 0
        content {
            CategoryEditorDialog(
                categoryState = CategoriesState(),
                onCategorySelected = { mutations++ },
                onAddCategory = { mutations++ },
                onDeleteCategory = { mutations++ },
                onEditCategory = { _, _ -> mutations++ },
                validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                onDismiss = { dismissals++ },
            )
        }
        rule.onNodeWithText(EnFeedFlowStrings.actionDone).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle {
            assertEquals(0, mutations)
            assertEquals(1, dismissals)
        }
    }

    @Test
    fun `category manager owns its name dialog and native Escape restores the initiating focus`() {
        var mutations = 0
        var dismissals = 0
        content {
            CategoryEditorDialog(
                categoryState = CategoriesState(),
                onCategorySelected = { mutations++ },
                onAddCategory = { mutations++ },
                onDeleteCategory = { mutations++ },
                onEditCategory = { _, _ -> mutations++ },
                validateCategoryName = { _, _ -> CategoryNameValidationResult.VALID },
                onDismiss = { dismissals++ },
            )
        }
        val add = rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_SHEET_ADD)
        add.requestFocus().performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag(FeedFormE2eIds.CATEGORY_ADD_INPUT).assertIsFocused()
        SwingUtilities.invokeAndWait {
            val parent = visibleDialog(EnFeedFlowStrings.addFeedCategoriesTitle)
            val child = visibleDialog(EnFeedFlowStrings.addFeedCategoryTitle)
            assertSame(parent, child.owner)
            val input = descendants(child).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val escape = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(escape) }
            assertTrue(escape.isConsumed)
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            var closed = false
            SwingUtilities.invokeAndWait {
                closed = Window.getWindows().filterIsInstance<Dialog>().none {
                    it.isVisible && it.title == EnFeedFlowStrings.addFeedCategoryTitle
                }
            }
            closed
        }
        add.assertIsFocused()
        SwingUtilities.invokeAndWait { assertTrue(visibleDialog(EnFeedFlowStrings.addFeedCategoriesTitle).isVisible) }
        rule.runOnIdle {
            assertEquals(0, mutations)
            assertEquals(0, dismissals)
        }
    }

    @Test
    fun `remove category confirmation explicitly preserves feeds and names its action`() {
        var deleted: CategoryId? = null
        val category = CategoryId("news")
        content { DrawerDeleteCategoryDialog(true, category, {}, { deleted = it }) }
        rule.onNodeWithText(EnFeedFlowStrings.deleteCategoryConfirmationMessage).assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.deleteCategory).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(category, deleted) }
    }

    @Test
    fun `remove feeds in category confirmation names its separate irreversible action`() {
        var deleted = 0
        content { DrawerDeleteAllFeedsDialog(true, {}, { deleted++ }) }
        rule.onNodeWithText(EnFeedFlowStrings.deleteAllFeedsConfirmationMessage).assertExists()
        rule.onNodeWithText(EnFeedFlowStrings.deleteAllFeedsInCategory)
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals(1, deleted) }
    }

    private fun activateTwice(label: String) {
        val activate = rule.onNodeWithText(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        rule.runOnIdle {
            activate()
            activate()
        }
    }

    private fun visibleDialog(title: String): Dialog = Window.getWindows().filterIsInstance<Dialog>().single {
        it.isVisible && it.title == title
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun content(body: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false, content = body)
            }
        }
    }

    @Composable
    private fun NameDialog(
        initial: String = "",
        title: String = EnFeedFlowStrings.addFeedCategoryTitle,
        categoryId: CategoryId? = null,
        enabled: Boolean = true,
        onSave: (CategoryName) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        CategoryNameDialog(
            visible = true,
            title = title,
            initialName = initial,
            categoryId = categoryId,
            validateCategoryName = { _, value ->
                if (value.name.isBlank() || initial == "External update") {
                    CategoryNameValidationResult.DUPLICATE
                } else {
                    CategoryNameValidationResult.VALID
                }
            },
            onDismiss = onDismiss,
            onSave = onSave,
            enabled = enabled,
        )
    }
}
