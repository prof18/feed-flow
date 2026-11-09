package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.Window
import com.prof18.feedflow.desktop.main.MainWindowSurface
import com.prof18.feedflow.desktop.ui.desktopMainWindowTitle
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import javax.swing.SwingUtilities
import kotlin.test.assertEquals

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
}
