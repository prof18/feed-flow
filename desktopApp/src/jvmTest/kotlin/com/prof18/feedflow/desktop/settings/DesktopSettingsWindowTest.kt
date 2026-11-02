package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixture
import com.prof18.feedflow.desktop.testing.NativeDesktopWindowFixtureRule
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.FeedFlowTheme
import com.prof18.feedflow.i18n.EnFeedFlowStrings
import org.jetbrains.jewel.ui.component.Text
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.awt.Desktop
import java.awt.Dialog
import java.awt.KeyboardFocusManager
import java.awt.Window
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DesktopSettingsWindowTest {
    private val activationRequest = mutableIntStateOf(0)
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                DesktopSettingsDialog(onCloseRequest = {}, activationRequest = activationRequest.value) { modifier ->
                    Text("Settings window content", modifier = modifier.testTag(WINDOW_CONTENT_TAG))
                }
            }
        }
    }

    private val nativeWindow = NativeDesktopWindowFixtureRule(fixture)
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(nativeWindow).around(compose)

    @Test
    fun `raising Settings preserves the existing native window and user size`() {
        val window = awaitSettingsWindow()
        val initialSize = readWindowSize(window)
        // Shrink rather than grow: on small displays (CI VMs) macOS asynchronously clamps a taller frame to the screen.
        val targetWidth = (initialSize.width - RESIZE_WIDTH_DP).coerceAtLeast(initialSize.minimumWidth)
        val targetHeight = (initialSize.height - RESIZE_HEIGHT_DP).coerceAtLeast(initialSize.minimumHeight)
        SwingUtilities.invokeAndWait { window.setSize(targetWidth, targetHeight) }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width == targetWidth && size.height == targetHeight
        }
        val resized = readWindowSize(window)
        compose.runOnIdle { activationRequest.value++ }
        compose.waitForIdle()
        assertSame(window, awaitSettingsWindow())
        val afterActivation = readWindowSize(window)
        assertEquals(resized.width, afterActivation.width)
        assertEquals(resized.height, afterActivation.height)
    }

    @Test
    fun `raising Settings gives the existing dialog keyboard focus`() {
        val window = awaitSettingsWindow()
        // The OS gives keyboard focus only to windows of the active application, and toFront() cannot steal
        // activation from another app (macOS refuses it by design). When another app is frontmost, as on a
        // shared machine running simulators or other agents, focus could never arrive. Activate the test JVM
        // first, and skip rather than fail when the OS refuses.
        assumeTrue(
            "The OS did not make the test JVM the active application, so window focus cannot be observed",
            awaitAppActive(),
        )
        compose.runOnIdle { activationRequest.value++ }
        compose.waitForIdle()
        assertSame(window, awaitSettingsWindow())
        var focused = false
        compose.waitUntil(timeoutMillis = WINDOW_START_TIMEOUT_MS) {
            SwingUtilities.invokeAndWait {
                // Another app may take activation back while waiting; only the in-app focus target is under test.
                if (!isAppActive()) requestAppForeground()
                focused = window.isFocused
            }
            focused
        }
    }

    private fun awaitAppActive(): Boolean {
        val deadline = System.currentTimeMillis() + WINDOW_START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            var active = false
            SwingUtilities.invokeAndWait {
                active = isAppActive()
                if (!active) requestAppForeground()
            }
            if (active) return true
            Thread.sleep(APP_ACTIVATION_POLL_MS)
        }
        return false
    }

    private fun isAppActive(): Boolean = KeyboardFocusManager.getCurrentKeyboardFocusManager().activeWindow != null

    private fun requestAppForeground() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
            Desktop.getDesktop().requestForeground(false)
        }
    }

    private fun awaitSettingsWindow(): Dialog {
        var window: Dialog? = null
        compose.waitUntil(timeoutMillis = WINDOW_START_TIMEOUT_MS) {
            SwingUtilities.invokeAndWait { window = settingsWindowOrNull() }
            window != null
        }
        return requireNotNull(window)
    }

    private fun settingsWindowOrNull(): Dialog? = Window.getWindows()
        .filterIsInstance<Dialog>()
        .singleOrNull { it.isVisible && it.title == EnFeedFlowStrings.settingsTitle }

    private fun readWindowSize(window: Dialog): NativeWindowSize {
        var size: NativeWindowSize? = null
        SwingUtilities.invokeAndWait {
            size = NativeWindowSize(
                width = window.width,
                height = window.height,
                minimumWidth = window.minimumSize.width,
                minimumHeight = window.minimumSize.height,
            )
        }
        return requireNotNull(size)
    }

    private companion object {
        const val WINDOW_CONTENT_TAG = "settings-window-content"
        const val RESIZE_WIDTH_DP = 80
        const val RESIZE_HEIGHT_DP = 60
        const val WINDOW_START_TIMEOUT_MS = 10_000L
        const val WINDOW_RESIZE_TIMEOUT_MS = 2_000L
        const val APP_ACTIVATION_POLL_MS = 100L
    }
}

private data class NativeWindowSize(
    val width: Int,
    val height: Int,
    val minimumWidth: Int,
    val minimumHeight: Int,
)
