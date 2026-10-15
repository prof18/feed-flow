@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.desktop.DesktopStartupSplashContent
import com.prof18.feedflow.desktop.DesktopStartupSplashWindow
import com.prof18.feedflow.desktop.SplashContent
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.awt.Dialog
import java.awt.Window
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopWindowLifecycleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `mounted native dialog backgrounds follow light dark and OLED without recreation or lost focus`() {
        val dark = mutableStateOf(false)
        val oled = mutableStateOf(false)
        var expected = Color.Unspecified
        rule.setContent {
            FeedFlowTheme(dark.value, oled.value) {
                val background = JewelTheme.globalColors.panelBackground
                SideEffect { expected = background }
                Dialog(title = "Theme fixture", onCloseRequest = {}) {
                    DefaultButton(onClick = {}, modifier = Modifier.testTag("theme-dialog-action")) { Text("Fixture") }
                }
            }
        }
        rule.waitUntilExactlyOneExists(hasTestTag("theme-dialog-action"), timeoutMillis = 5_000)
        rule.onNodeWithTag("theme-dialog-action").requestFocus()
        val first = dialog()
        rule.runOnIdle { dark.value = true }
        rule.waitForIdle()
        awaitBackground(expected)
        assertBackground(expected)
        rule.runOnIdle { oled.value = true }
        rule.waitForIdle()
        awaitBackground(Color.Black)
        assertBackground(Color.Black)
        assertSame(first, dialog())
        rule.onNodeWithTag("theme-dialog-action").assertIsFocused()
    }

    @Test
    fun `splash honors the selected dark and OLED theme independently of the system theme`() {
        val oled = mutableStateOf(false)
        rule.setContent {
            SplashContent(
                modifier = Modifier.testTag("splash-fixture"),
                useRoundedCorners = false,
                darkTheme = true,
                useOledTheme = oled.value,
            )
        }
        assertSplashBackground(Color(color = 0xFF1E1F22))
        rule.runOnIdle { oled.value = true }
        assertSplashBackground(Color.Black)
    }

    @Test
    fun `startup splash stays hidden until the saved preference supplies its first visible theme`() {
        val theme = mutableStateOf<ThemeMode?>(null)
        val systemDark = mutableStateOf(false)
        rule.setContent {
            DesktopStartupSplashWindow(
                themeMode = theme.value,
                systemDarkTheme = systemDark.value,
                progress = 0f,
                onCloseRequest = {},
                modifier = Modifier.testTag("startup-splash-fixture"),
            )
            theme.value?.let { mode ->
                DesktopStartupSplashContent(
                    themeMode = mode,
                    systemDarkTheme = systemDark.value,
                    progress = 0f,
                    useRoundedCorners = false,
                    modifier = Modifier.testTag("startup-splash-body-fixture"),
                )
            }
        }
        rule.waitForIdle()
        SwingUtilities.invokeAndWait { assertTrue(startupWindows().none { it.isShowing }) }
        listOf(
            ThemeMode.OLED to Color.Black,
            ThemeMode.DARK to Color(color = 0xFF1E1F22),
            ThemeMode.LIGHT to Color.White,
            ThemeMode.SYSTEM to Color.White,
        ).forEach { (mode, color) ->
            rule.runOnIdle {
                systemDark.value = mode == ThemeMode.LIGHT
                theme.value = mode
            }
            rule.waitUntil(timeoutMillis = 5_000) {
                var visible = false
                SwingUtilities.invokeAndWait { visible = startupWindows().singleOrNull()?.isShowing == true }
                visible
            }
            rule.waitForIdle()
            assertEquals(color, startupPixel())
            rule.runOnIdle { theme.value = null }
            rule.waitUntil(timeoutMillis = 5_000) {
                var hidden = false
                SwingUtilities.invokeAndWait { hidden = startupWindows().none { it.isShowing } }
                hidden
            }
        }
        rule.runOnIdle {
            systemDark.value = true
            theme.value = ThemeMode.SYSTEM
        }
        rule.waitForIdle()
        assertEquals(Color(color = 0xFF1E1F22), startupPixel())
    }

    private fun assertSplashBackground(expected: Color) {
        val pixels = rule.onNodeWithTag("splash-fixture").captureToImage().toPixelMap()
        assertEquals(expected, pixels[BACKGROUND_SAMPLE, BACKGROUND_SAMPLE])
    }

    private fun assertBackground(expected: Color) {
        SwingUtilities.invokeAndWait {
            val window = dialog()
            assertEquals(expected.toArgb(), window.background.rgb)
            val root = window as javax.swing.RootPaneContainer
            assertEquals(expected.toArgb(), root.contentPane.background.rgb)
            assertEquals(expected.toArgb(), root.rootPane.background.rgb)
        }
    }

    private fun awaitBackground(expected: Color) {
        rule.waitUntil(timeoutMillis = 5_000) {
            var matches = false
            SwingUtilities.invokeAndWait { matches = dialog().background.rgb == expected.toArgb() }
            matches
        }
    }

    private fun dialog(): Dialog = Window.getWindows().filterIsInstance<Dialog>().single {
        it.isVisible && it.title == "Theme fixture"
    }

    private fun startupWindows(): List<ComposeWindow> = Window.getWindows().filterIsInstance<ComposeWindow>()
        .filter { it.title == "FeedFlow" && it.isDisplayable }

    private fun startupPixel(): Color {
        val pixels = rule.onNodeWithTag("startup-splash-body-fixture").captureToImage().toPixelMap()
        return pixels[STARTUP_BACKGROUND_SAMPLE, STARTUP_BACKGROUND_SAMPLE]
    }

    private companion object {
        const val BACKGROUND_SAMPLE = 5
        const val STARTUP_BACKGROUND_SAMPLE = 24
    }
}
