@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.window.Window
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs
import com.prof18.feedflow.desktop.main.MainWindowSurface
import com.prof18.feedflow.desktop.testing.requestNativeTestAppForeground
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.desktopMainWindowTitle
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.awt.Dialog
import java.awt.KeyboardFocusManager
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopMainWindowFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `main frame exposes the app name to native accessibility`() {
        rule.setContent {
            Window(title = desktopMainWindowTitle(), onCloseRequest = {}) {
                FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                    MainWindowSurface(modifier = Modifier.fillMaxSize()) {
                        Text("Native frame title fixture", modifier = Modifier.testTag("main-native-title-fixture"))
                    }
                }
            }
        }
        rule.onNodeWithTag("main-native-title-fixture").assertExists()
        SwingUtilities.invokeAndWait {
            val frame = java.awt.Window.getWindows().filterIsInstance<ComposeWindow>().single { it.isShowing }
            assertEquals(EnFeedFlowStrings.appName, frame.title)
            assertEquals(EnFeedFlowStrings.appName, frame.accessibleContext.accessibleName)
        }
    }

    @Test
    fun `main window restores its second launcher after cancelling and reopening a native child`() {
        val visible = mutableStateOf(false)
        val ownerVisible = mutableStateOf(true)
        rule.setContent {
            if (!ownerVisible.value) return@setContent
            Window(title = FRAME_TITLE, onCloseRequest = { ownerVisible.value = false }) {
                CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
                    FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                        MainWindowSurface(modifier = Modifier.fillMaxSize()) {
                            Column {
                                OutlinedButton(onClick = {}, modifier = Modifier.testTag(OTHER_TAG)) {
                                    Text("Other frame action")
                                }
                                OutlinedButton(
                                    onClick = { visible.value = true },
                                    modifier = Modifier.testTag(LAUNCHER_TAG),
                                ) { Text("Open frame confirmation") }
                            }
                        }
                        if (visible.value) {
                            ConfirmationDialog(
                                title = EnFeedFlowStrings.deleteFeedConfirmationTitle,
                                message = EnFeedFlowStrings.deleteFeedConfirmationMessage,
                                onConfirm = { error("This fixture only cancels") },
                                onDismiss = { visible.value = false },
                            )
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag(LAUNCHER_TAG).assertExists()
        val frame = java.awt.Window.getWindows().filterIsInstance<ComposeWindow>().single {
            it.isShowing && it.title == FRAME_TITLE
        }
        SwingUtilities.invokeAndWait {
            requestNativeTestAppForeground()
            frame.toFront()
            frame.requestFocus()
        }
        awaitNativeFocus(frame)
        rule.onNodeWithTag(OTHER_TAG).requestFocus().assertIsFocused()
        rule.onNodeWithTag(LAUNCHER_TAG).requestFocus().assertIsFocused()
        var previous: ComposeDialog? = null
        repeat(2) {
            rule.onNodeWithTag(LAUNCHER_TAG).performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
            val child = java.awt.Window.getWindows().filterIsInstance<ComposeDialog>().single {
                it.isShowing && it.title == alertTitle(EnFeedFlowStrings.deleteFeedConfirmationTitle)
            }
            SwingUtilities.invokeAndWait {
                assertSame(frame, child.owner)
                assertEquals(Dialog.ModalityType.DOCUMENT_MODAL, child.modalityType)
                previous?.let { assertNotSame(it, child) }
            }
            awaitNativeFocus(child)
            rule.onNodeWithText(EnFeedFlowStrings.cancelButton).assertIsFocused()
            rule.onNodeWithText(EnFeedFlowStrings.cancelButton)
                .performSemanticsAction(SemanticsActions.OnClick) { it() }
            rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
            rule.waitUntil(timeoutMillis = 5_000) {
                var disposed = false
                SwingUtilities.invokeAndWait { disposed = !child.isDisplayable }
                disposed
            }
            awaitNativeFocus(frame)
            rule.waitUntil(timeoutMillis = 5_000) {
                rule.onNodeWithTag(LAUNCHER_TAG).fetchSemanticsNode().config[SemanticsProperties.Focused]
            }
            rule.onNodeWithTag(LAUNCHER_TAG).assertIsFocused()
            SwingUtilities.invokeAndWait { assertTrue(frame.isShowing) }
            previous = child
        }
        rule.onNodeWithTag(LAUNCHER_TAG).performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.waitUntilExactlyOneExists(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        val child = java.awt.Window.getWindows().filterIsInstance<ComposeDialog>().single {
            it.isShowing && it.title == alertTitle(EnFeedFlowStrings.deleteFeedConfirmationTitle)
        }
        awaitNativeFocus(child)
        SwingUtilities.invokeAndWait {
            frame.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
        }
        rule.waitUntilDoesNotExist(hasText(EnFeedFlowStrings.cancelButton), timeoutMillis = 5_000)
        rule.waitUntil(timeoutMillis = 5_000) {
            var disposed = false
            SwingUtilities.invokeAndWait { disposed = !frame.isDisplayable && !child.isDisplayable }
            disposed
        }
    }

    // Off macOS, alert dialogs carry the app name in the native title bar instead of their question.
    private fun alertTitle(title: String): String = nativeWindowTitle(
        title = title,
        alertStyle = true,
        isMacOs = getDesktopOS().isMacOs(),
        appName = EnFeedFlowStrings.appName,
    )

    private fun awaitNativeFocus(window: java.awt.Window) {
        rule.waitUntil(timeoutMillis = 5_000) {
            var focused = false
            SwingUtilities.invokeAndWait {
                focused = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusedWindow === window
            }
            focused
        }
    }

    private companion object {
        const val FRAME_TITLE = "Main window focus fixture"
        const val LAUNCHER_TAG = "frame-confirmation-launcher"
        const val OTHER_TAG = "frame-other-action"
    }
}
