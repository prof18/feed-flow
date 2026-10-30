@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.home

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import com.prof18.feedflow.core.model.FeedFilter
import com.prof18.feedflow.desktop.home.menubar.FileMenu
import com.prof18.feedflow.desktop.home.menubar.MenuBarActions
import com.prof18.feedflow.desktop.home.menubar.MenuBarState
import com.prof18.feedflow.desktop.ui.DesktopUiEnvironment
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.junit.Rule
import org.junit.Test
import javax.swing.JMenuItem
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWorkspaceMenuTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `File menu leaves Accounts to Settings`() {
        var nativeWindow: ComposeWindow? = null
        rule.setContent {
            DesktopUiEnvironment(darkTheme = false, currentLanguageTag = "en") {
                Window(onCloseRequest = {}, title = EnFeedFlowStrings.appName) {
                    nativeWindow = window
                    MenuBar {
                        FileMenu(
                            isMacOS = true,
                            state = MenuBarState(
                                showDebugMenu = false,
                                feedFilter = FeedFilter.Timeline,
                                isSyncUploadRequired = false,
                                commands = DesktopWorkspaceCommandAvailability(false, false),
                            ),
                            actions = MenuBarActions({}, {}, {}, {}, {}, {}, {}, {}),
                        )
                    }
                }
            }
        }
        rule.waitForIdle()

        SwingUtilities.invokeAndWait {
            val menu = requireNotNull(nativeWindow).jMenuBar.getMenu(0)
            val labels = (0 until menu.itemCount).mapNotNull(menu::getItem).map { it.text }
            assertFalse(EnFeedFlowStrings.settingsAccounts in labels, "Accounts belongs to Settings, not the File menu")
            assertTrue(EnFeedFlowStrings.importExportLabel in labels)
        }
    }

    @Test
    fun `native menu dispatches once follows loading state and leaves editor text alone`() {
        val commands = mutableStateOf(DesktopWorkspaceCommandAvailability())
        val text = mutableStateOf("")
        var nativeWindow: ComposeWindow? = null
        var refreshes = 0
        var bulkActions = 0
        val refresh = desktopWorkspaceAction(DesktopWorkspaceCommand.Refresh, { commands.value }) { refreshes++ }
        val bulk = desktopWorkspaceAction(DesktopWorkspaceCommand.BulkAction, { commands.value }) { bulkActions++ }
        rule.setContent {
            DesktopUiEnvironment(darkTheme = false, currentLanguageTag = "en") {
                Window(onCloseRequest = {}, title = EnFeedFlowStrings.appName) {
                    nativeWindow = window
                    MenuBar {
                        FileMenu(
                            isMacOS = true,
                            state = MenuBarState(false, FeedFilter.Timeline, false, commands.value),
                            actions = MenuBarActions(refresh, {}, bulk, {}, {}, {}, {}, {}),
                        )
                    }
                    BasicTextField(text.value, { text.value = it }, Modifier.testTag("editor"))
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("editor").performTextInput("r日本語")
        rule.waitUntilExactlyOneExists(hasText("r日本語"), timeoutMillis = 5_000L)
        rule.onNodeWithTag("editor").assertTextEquals("r日本語")
        rule.runOnIdle {
            assertEquals(0, refreshes)
            assertEquals(0, bulkActions)
        }
        SwingUtilities.invokeAndWait {
            val menu = requireNotNull(nativeWindow).jMenuBar.getMenu(0)
            fun item(label: String): JMenuItem = (0 until menu.itemCount)
                .mapNotNull(menu::getItem).single { it.text == label }
            item(EnFeedFlowStrings.refreshFeeds).doClick()
            item(EnFeedFlowStrings.markAllReadButton).doClick()
            assertFalse(
                (0 until menu.itemCount).mapNotNull(menu::getItem)
                    .any { it.text == EnFeedFlowStrings.clearOldArticlesButton },
            )
        }
        rule.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, bulkActions)
            commands.value = DesktopWorkspaceCommandAvailability(false, false)
        }
        rule.waitUntil(timeoutMillis = 5_000L) {
            var disabled = false
            SwingUtilities.invokeAndWait {
                val menu = requireNotNull(nativeWindow).jMenuBar.getMenu(0)
                disabled = (0 until menu.itemCount).mapNotNull(menu::getItem)
                    .single { it.text == EnFeedFlowStrings.refreshFeeds }.isEnabled.not()
            }
            disabled
        }
        SwingUtilities.invokeAndWait {
            val menu = requireNotNull(nativeWindow).jMenuBar.getMenu(0)
            val gatedLabels = setOf(
                EnFeedFlowStrings.refreshFeeds,
                EnFeedFlowStrings.markAllReadButton,
            )
            (0 until menu.itemCount).mapNotNull(menu::getItem).forEach { item ->
                if (item.text in gatedLabels) {
                    assertFalse(item.isEnabled, item.text)
                    item.doClick()
                } else {
                    assertTrue(item.isEnabled, item.text)
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, bulkActions)
        }
    }
}
