@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.home.NoFeedsDialog
import com.prof18.feedflow.desktop.home.drawer.DesktopFeedMoveDialog
import com.prof18.feedflow.desktop.settings.DesktopSettingsDialog
import com.prof18.feedflow.desktop.testing.DesktopLibraryFixture
import com.prof18.feedflow.desktop.testing.requestNativeTestAppForeground
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.Dialog
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DialogKeyboardTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `empty library window initially focuses Add feed and Enter dismisses before opening it`() {
        val actions = mutableListOf<String>()
        noFeeds(actions)
        rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsFocused()
        press(dialog(EnFeedFlowStrings.noFeedModalTitle), KeyEvent.VK_ENTER)
        rule.runOnIdle { assertEquals(listOf("dismiss", "add"), actions) }
    }

    @Test
    fun `empty library native Escape dismisses without choosing a setup action`() {
        val actions = mutableListOf<String>()
        noFeeds(actions)
        press(dialog(EnFeedFlowStrings.noFeedModalTitle), KeyEvent.VK_ESCAPE)
        rule.runOnIdle { assertEquals(listOf("dismiss"), actions) }
    }

    @Test
    fun `move dialog starts on destination and keyboard confirms the intended source and category`() {
        val library = DesktopLibraryFixture.load()
        val source = library.sources.first { it.feedSource.category != null }.feedSource
        val category = library.categories.first { it.id != source.category?.id }
        val visible = mutableStateOf(true)
        val actions = mutableListOf<String>()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    if (visible.value) {
                        DesktopFeedMoveDialog(
                            categories = persistentListOf(category),
                            feedSources = persistentListOf(source),
                            onMove = { sources, destination ->
                                actions += "${sources.single().id}:${destination?.id}"
                                visible.value = false
                            },
                            onDismiss = { actions += "dismiss" },
                        )
                    }
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)).assertIsFocused()
        val window = dialog(EnFeedFlowStrings.changeCategory)
        press(window, KeyEvent.VK_DOWN)
        press(window, KeyEvent.VK_DOWN)
        press(window, KeyEvent.VK_ENTER)
        rule.waitUntilExactlyOneExists(hasText(category.title), timeoutMillis = 5_000)
        rule.onNodeWithText(category.title).assertExists()
        press(window, KeyEvent.VK_TAB)
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()
        press(window, KeyEvent.VK_TAB)
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).assertIsFocused()
        press(window, KeyEvent.VK_ENTER)
        rule.runOnIdle { assertEquals(listOf("${source.id}:${category.id}"), actions) }
    }

    @Test
    fun `move dialog Escape closes its destination popup before dismissing without a move`() {
        val library = DesktopLibraryFixture.load()
        val source = library.sources.first { it.feedSource.category != null }.feedSource
        val category = library.categories.first { it.id != source.category?.id }
        val visible = mutableStateOf(true)
        val actions = mutableListOf<String>()
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    if (visible.value) {
                        DesktopFeedMoveDialog(
                            categories = persistentListOf(category),
                            feedSources = persistentListOf(source),
                            onMove = { _, _ -> actions += "move" },
                            onDismiss = {
                                actions += "dismiss"
                                visible.value = false
                            },
                        )
                    }
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        val window = dialog(EnFeedFlowStrings.changeCategory)
        press(window, KeyEvent.VK_DOWN)
        rule.waitUntilExactlyOneExists(hasText(category.title), timeoutMillis = 5_000)
        press(window, KeyEvent.VK_ESCAPE)
        rule.waitUntilDoesNotExist(hasText(category.title), timeoutMillis = 5_000)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)).assertIsFocused()
        rule.runOnIdle { assertTrue(actions.isEmpty()) }
        press(window, KeyEvent.VK_ESCAPE)
        rule.runOnIdle { assertEquals(listOf("dismiss"), actions) }
    }

    @Test
    fun `empty library Tab and Shift Tab traverse all actions and wrap`() {
        val actions = mutableListOf<String>()
        noFeeds(actions)
        val window = dialog(EnFeedFlowStrings.noFeedModalTitle)
        val labels = listOf(
            EnFeedFlowStrings.addFeed,
            EnFeedFlowStrings.importExportLabel,
            EnFeedFlowStrings.settingsAccounts,
            EnFeedFlowStrings.feedSuggestionsTitle,
        )
        (labels.drop(1) + labels.first()).forEach { label ->
            press(window, KeyEvent.VK_TAB)
            rule.onNodeWithText(label).assertIsFocused()
        }
        (labels.drop(1).reversed() + labels.first()).forEach { label ->
            press(window, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
            rule.onNodeWithText(label).assertIsFocused()
        }
        rule.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun `empty library keyboard import dismisses before opening import export`() {
        chooseNoFeedsAction(tabCount = 1, label = EnFeedFlowStrings.importExportLabel, action = "import")
    }

    @Test
    fun `empty library keyboard accounts dismisses before opening accounts`() {
        chooseNoFeedsAction(tabCount = 2, label = EnFeedFlowStrings.settingsAccounts, action = "accounts")
    }

    @Test
    fun `empty library keyboard suggestions dismisses before opening suggestions`() {
        chooseNoFeedsAction(tabCount = 3, label = EnFeedFlowStrings.feedSuggestionsTitle, action = "suggestions")
    }

    @Test
    fun `reopened empty library chooser retains its owner and returns launcher focus`() {
        val visible = mutableStateOf(false)
        var chosen = 0
        setOwnedDialogContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Dialog(title = OWNER_TITLE, size = DpSize(700.dp, 500.dp), onCloseRequest = {}) {
                        OutlinedButton(
                            onClick = { visible.value = true },
                            modifier = Modifier.testTag(LAUNCHER_TAG),
                        ) { Text("Open setup") }
                        NoFeedsDialog(
                            showDialog = visible.value,
                            onDismissRequest = { visible.value = false },
                            onAddFeedClick = { chosen++ },
                            onImportExportClick = { chosen++ },
                            onAccountsClick = { chosen++ },
                            onFeedSuggestionsClick = { chosen++ },
                        )
                    }
                }
            }
        }
        val owner = dialog(OWNER_TITLE)
        SwingUtilities.invokeAndWait {
            assertTrue(owner.owner is ComposeWindow && owner.owner.isShowing)
            requestNativeTestAppForeground()
            owner.toFront()
            owner.requestFocus()
        }
        awaitNativeFocus(owner)
        rule.onNodeWithTag(LAUNCHER_TAG).requestFocus().assertIsFocused()
        var previous: ComposeDialog? = null
        repeat(2) {
            rule.onNodeWithTag(LAUNCHER_TAG).performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.addFeed), timeoutMillis = 5_000)
            val child = dialog(EnFeedFlowStrings.noFeedModalTitle)
            SwingUtilities.invokeAndWait {
                assertSame(owner, child.owner)
                assertEquals(Dialog.ModalityType.DOCUMENT_MODAL, child.modalityType)
                previous?.let { prior -> assertNotSame(prior, child) }
            }
            rule.onNodeWithText(EnFeedFlowStrings.addFeed).assertIsFocused()
            awaitNativeFocus(child)
            SwingUtilities.invokeAndWait { owner.requestFocus() }
            rule.waitForIdle()
            SwingUtilities.invokeAndWait {
                assertSame(child, KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow)
            }
            press(child, KeyEvent.VK_TAB)
            rule.onNodeWithText(EnFeedFlowStrings.importExportLabel).assertIsFocused()
            press(child, KeyEvent.VK_ESCAPE)
            rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.addFeed), timeoutMillis = 5_000)
            awaitNativeFocus(owner)
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onNodeWithTag(LAUNCHER_TAG).fetchSemanticsNode().config[SemanticsProperties.Focused]
            }
            rule.onNodeWithTag(LAUNCHER_TAG).assertIsFocused()
            rule.runOnIdle { assertEquals(0, chosen) }
            previous = child
        }
    }

    private fun chooseNoFeedsAction(tabCount: Int, label: String, action: String) {
        val actions = mutableListOf<String>()
        noFeeds(actions)
        val window = dialog(EnFeedFlowStrings.noFeedModalTitle)
        repeat(tabCount) { press(window, KeyEvent.VK_TAB) }
        rule.onNodeWithText(label).assertIsFocused()
        press(window, KeyEvent.VK_ENTER)
        rule.runOnIdle { assertEquals(listOf("dismiss", action), actions) }
        rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.addFeed), timeoutMillis = 5_000)
    }

    private fun noFeeds(actions: MutableList<String>) {
        val visible = mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    NoFeedsDialog(
                        showDialog = visible.value,
                        onDismissRequest = {
                            actions += "dismiss"
                            visible.value = false
                        },
                        onAddFeedClick = { actions += "add" },
                        onImportExportClick = { actions += "import" },
                        onAccountsClick = { actions += "accounts" },
                        onFeedSuggestionsClick = { actions += "suggestions" },
                    )
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.addFeed), timeoutMillis = 5_000)
    }

    @Test
    fun `native Escape closes the actual settings wrapper`() {
        val dismissed = mutableStateOf(false)
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    if (!dismissed.value) {
                        DesktopSettingsDialog(onCloseRequest = { dismissed.value = true }) {
                            OutlinedButton(onClick = {}, modifier = Modifier.testTag("settings-escape-fixture")) {
                                Text("Settings keyboard fixture")
                            }
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("settings-escape-fixture").requestFocus()
        press(dialog(EnFeedFlowStrings.settingsTitle), KeyEvent.VK_ESCAPE)
        rule.runOnIdle { assertTrue(dismissed.value) }
    }

    @Test
    fun `caller consuming Escape keeps its native dialog open`() {
        var dismissed = false
        rule.setContent {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                DesktopDialogWindow(
                    title = "Consumed Escape fixture",
                    size = DpSize(400.dp, 300.dp),
                    onCloseRequest = { dismissed = true },
                    onKeyEvent = { true },
                ) {
                    OutlinedButton(onClick = {}, modifier = Modifier.testTag("consumed-escape-fixture")) {
                        Text("Consumed keyboard fixture")
                    }
                }
            }
        }
        rule.onNodeWithTag("consumed-escape-fixture").requestFocus()
        press(dialog("Consumed Escape fixture"), KeyEvent.VK_ESCAPE)
        rule.runOnIdle { assertFalse(dismissed) }
    }

    @Test
    fun `confirmation starts on Cancel and Enter cancels without mutating data`() {
        var confirmed = 0
        var dismissed = 0
        confirmation(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        awaitActions()
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()

        press(dialog(alertTitle(EnFeedFlowStrings.deleteFeedConfirmationTitle)), KeyEvent.VK_ENTER)

        rule.runOnIdle {
            assertEquals(0, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun `Tab reaches confirmation and repeated Enter dispatches only once`() {
        var confirmed = 0
        var dismissed = 0
        confirmation(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
        awaitActions()
        val window = dialog(alertTitle(EnFeedFlowStrings.deleteFeedConfirmationTitle))
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()

        press(window, KeyEvent.VK_TAB)
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).assertIsFocused()
        press(window, KeyEvent.VK_TAB)
        rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()
        press(window, KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
        rule.onNodeWithText(EnFeedFlowStrings.confirmButton).assertIsFocused()
        press(window, KeyEvent.VK_ENTER)
        press(window, KeyEvent.VK_ENTER)

        rule.runOnIdle {
            assertEquals(1, confirmed)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun `reopened confirmation retains its native owner and returns focus to its launcher`() {
        val visible = mutableStateOf(false)
        setOwnedDialogContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    Dialog(title = OWNER_TITLE, size = DpSize(700.dp, 500.dp), onCloseRequest = {}) {
                        OutlinedButton(onClick = {}, modifier = Modifier.testTag(OTHER_ACTION_TAG)) {
                            Text("Another owner action")
                        }
                        OutlinedButton(
                            onClick = { visible.value = true },
                            modifier = Modifier.testTag(LAUNCHER_TAG),
                        ) { Text("Open confirmation") }
                        if (visible.value) {
                            ConfirmationDialog(
                                title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                                message = EnFeedFlowStrings.deleteFeedConfirmationMessage,
                                onConfirm = { error("This journey only cancels") },
                                onDismiss = { visible.value = false },
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag(LAUNCHER_TAG).assertExists()
        val owner = dialog(OWNER_TITLE)
        SwingUtilities.invokeAndWait {
            assertTrue(owner.owner is ComposeWindow && owner.owner.isShowing)
            requestNativeTestAppForeground()
            owner.toFront()
            owner.requestFocus()
        }
        awaitNativeFocus(owner)
        rule.onNodeWithTag(OTHER_ACTION_TAG).requestFocus().assertIsFocused()
        rule.onNodeWithTag(LAUNCHER_TAG).requestFocus().assertIsFocused()
        var previous: ComposeDialog? = null
        repeat(2) {
            rule.onNodeWithTag(LAUNCHER_TAG).performSemanticsAction(SemanticsActions.OnClick) { it() }
            awaitActions()
            val child = dialog(alertTitle(EnFeedFlowStrings.deleteFeedConfirmationTitle))
            SwingUtilities.invokeAndWait {
                assertSame(owner, child.owner)
                assertEquals(Dialog.ModalityType.DOCUMENT_MODAL, child.modalityType)
                previous?.let { prior -> assertNotSame(prior, child) }
            }
            rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()
            awaitNativeFocus(child)
            SwingUtilities.invokeAndWait { owner.requestFocus() }
            rule.waitForIdle()
            SwingUtilities.invokeAndWait {
                assertSame(child, KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow)
                assertFalse(owner.isFocused)
            }
            press(child, KeyEvent.VK_ESCAPE)
            rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
            rule.waitUntil(timeoutMillis = 5_000) {
                var disposed = false
                SwingUtilities.invokeAndWait { disposed = !child.isDisplayable }
                disposed
            }
            awaitNativeFocus(owner)
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onNodeWithTag(LAUNCHER_TAG).fetchSemanticsNode().config[SemanticsProperties.Focused]
            }
            rule.onNodeWithTag(LAUNCHER_TAG).assertIsFocused()
            SwingUtilities.invokeAndWait { assertTrue(owner.isShowing) }
            previous = child
        }
    }

    @Test
    fun `native Escape dismisses confirmation without mutating data`() {
        var dismissed = false
        var confirmed = false
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
        rule.waitForIdle()
        SwingUtilities.invokeAndWait {
            val dialog = Window.getWindows().filterIsInstance<ComposeDialog>().single { it.isShowing }
            val input = descendants(dialog).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            val escape = KeyEvent(input, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE)
            input.keyListeners.forEach { it.keyPressed(escape) }
        }
        rule.runOnIdle {
            assertTrue(dismissed)
            assertFalse(confirmed)
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun confirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    ConfirmationDialog(
                        title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                        message = EnFeedFlowStrings.deleteFeedConfirmationMessage,
                        onConfirm = onConfirm,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }

    private fun setOwnedDialogContent(content: @Composable () -> Unit) {
        rule.setContent {
            androidx.compose.ui.window.Window(title = OWNER_FRAME_TITLE, onCloseRequest = {}) {
                content()
            }
        }
    }

    private fun awaitActions() {
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
    }

    // Off macOS, alert dialogs carry the app name in the native title bar instead of their question.
    private fun alertTitle(title: String): String = nativeWindowTitle(
        title = title,
        alertStyle = true,
        isMacOs = getDesktopOS().isMacOs(),
        appName = EnFeedFlowStrings.appName,
    )

    private fun dialog(title: String): ComposeDialog = Window.getWindows().filterIsInstance<ComposeDialog>().single {
        it.isShowing && it.title == title
    }

    private fun awaitNativeFocus(window: Window) {
        rule.waitUntil(timeoutMillis = 5_000) {
            var focused = false
            SwingUtilities.invokeAndWait {
                focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow === window
            }
            focused
        }
    }

    private fun press(window: ComposeDialog, key: Int, modifiers: Int = 0) {
        SwingUtilities.invokeAndWait {
            val input = descendants(window).single { component ->
                component.keyListeners.any { it.javaClass.name.contains("ComposeSceneMediator") }
            }
            listOf(KeyEvent.KEY_PRESSED, KeyEvent.KEY_RELEASED).forEach { type ->
                val event = KeyEvent(input, type, System.currentTimeMillis(), modifiers, key)
                input.keyListeners.forEach { listener ->
                    if (type == KeyEvent.KEY_PRESSED) listener.keyPressed(event) else listener.keyReleased(event)
                }
            }
        }
        rule.waitForIdle()
    }

    private companion object {
        const val OWNER_FRAME_TITLE = "Native owner frame fixture"
        const val OWNER_TITLE = "Confirmation owner fixture"
        const val LAUNCHER_TAG = "confirmation-launcher"
        const val OTHER_ACTION_TAG = "confirmation-other-action"
    }
}
