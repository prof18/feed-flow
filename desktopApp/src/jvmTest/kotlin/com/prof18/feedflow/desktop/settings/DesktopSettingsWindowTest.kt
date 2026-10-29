package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
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
import java.awt.GraphicsEnvironment
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DesktopSettingsWindowTest {
    private val density = AtomicReference<Float>()
    private val activationRequest = mutableIntStateOf(0)
    private val fixture = NativeDesktopWindowFixture {
        CompositionLocalProvider(LocalFeedFlowStrings provides EnFeedFlowStrings) {
            FeedFlowTheme(darkTheme = false, useOledTheme = false) {
                density.set(LocalDensity.current.density)
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

    @Test
    fun `settings dialog is resizable and enforces desktop minimum size`() {
        val window = awaitSettingsWindow()
        val initialSize = readWindowSize(window)
        val scale = initialSize.scale
        val densityScale = requireNotNull(density.get()) / scale
        // The OS keeps a window inside the usable screen, so small displays (CI VMs) get a smaller initial size.
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        assertTrue(initialSize.isResizable)
        assertTrue(initialSize.width >= minOf((INITIAL_WIDTH_DP * densityScale).toInt(), screen.width))
        assertTrue(initialSize.height >= minOf((INITIAL_HEIGHT_DP * densityScale).toInt(), screen.height))
        assertTrue(initialSize.minimumWidth >= (MINIMUM_WIDTH_DP * densityScale).toInt())
        assertTrue(initialSize.minimumHeight >= (MINIMUM_HEIGHT_DP * densityScale).toInt())

        SwingUtilities.invokeAndWait {
            window.setSize(initialSize.minimumWidth, initialSize.minimumHeight)
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width == initialSize.minimumWidth && size.height == initialSize.minimumHeight
        }
        val resizeStart = readWindowSize(window)
        SwingUtilities.invokeAndWait {
            window.setSize(
                resizeStart.width + (RESIZE_WIDTH_DP * densityScale).toInt(),
                resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt(),
            )
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width >= resizeStart.width + (RESIZE_WIDTH_DP * densityScale).toInt() &&
                size.height >= resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt()
        }
        val enlargedSize = readWindowSize(window)
        assertTrue(
            enlargedSize.height >= resizeStart.height + (RESIZE_HEIGHT_DP * densityScale).toInt(),
            "Resize from $resizeStart produced $enlargedSize at density $densityScale",
        )

        SwingUtilities.invokeAndWait {
            window.setSize(
                initialSize.minimumWidth - (BELOW_MINIMUM_DP * densityScale).toInt(),
                initialSize.minimumHeight - (BELOW_MINIMUM_DP * densityScale).toInt(),
            )
        }
        compose.waitUntil(timeoutMillis = WINDOW_RESIZE_TIMEOUT_MS) {
            val size = readWindowSize(window)
            size.width >= initialSize.minimumWidth && size.height >= initialSize.minimumHeight
        }
        val clampedSize = readWindowSize(window)
        assertTrue(clampedSize.width >= initialSize.minimumWidth)
        assertTrue(clampedSize.height >= initialSize.minimumHeight)
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
            val transform = window.graphicsConfiguration.defaultTransform
            size = NativeWindowSize(
                width = window.width,
                height = window.height,
                minimumWidth = window.minimumSize.width,
                minimumHeight = window.minimumSize.height,
                scale = transform.scaleX,
                isResizable = window.isResizable,
            )
        }
        return requireNotNull(size)
    }

    private companion object {
        const val WINDOW_CONTENT_TAG = "settings-window-content"
        const val INITIAL_WIDTH_DP = 960
        const val INITIAL_HEIGHT_DP = 720
        const val MINIMUM_WIDTH_DP = 680
        const val MINIMUM_HEIGHT_DP = 520
        const val RESIZE_WIDTH_DP = 80
        const val RESIZE_HEIGHT_DP = 60
        const val BELOW_MINIMUM_DP = 100
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
    val scale: Double,
    val isResizable: Boolean,
)
