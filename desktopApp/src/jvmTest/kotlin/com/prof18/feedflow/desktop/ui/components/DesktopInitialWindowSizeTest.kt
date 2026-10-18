@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import org.jetbrains.jewel.ui.component.Text
import org.junit.Rule
import org.junit.Test
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class DesktopInitialWindowSizeTest {
    @get:Rule
    val rule = createComposeRule()
    private val openedFrame = AtomicReference<ComposeWindow?>()

    @Test
    fun `decorated frame retains initial and user resized bounds when reopened`() {
        val visible = mutableStateOf(true)
        val state = WindowState(size = DpSize(INITIAL_WIDTH.dp, INITIAL_HEIGHT.dp))
        rule.setContent {
            if (visible.value) {
                SwingWindow(
                    title = TITLE,
                    onCloseRequest = {},
                    state = state,
                    init = { window ->
                        window.restoreInitialLinuxWindowSize(state.size)
                        window.addWindowListener(object : WindowAdapter() {
                            override fun windowOpened(event: WindowEvent) {
                                openedFrame.set(window)
                            }
                        })
                    },
                ) {
                    FeedFlowTheme(darkTheme = false, useOledTheme = false) { Text(TITLE) }
                }
            }
        }
        rule.onNodeWithText(TITLE).assertExists()
        val first = frame()
        assertBounds(first, INITIAL_WIDTH, INITIAL_HEIGHT)
        SwingUtilities.invokeAndWait { first.setSize(RESIZED_WIDTH, RESIZED_HEIGHT) }
        rule.waitUntil(timeoutMillis = WINDOW_TIMEOUT_MS) {
            state.size == DpSize(RESIZED_WIDTH.dp, RESIZED_HEIGHT.dp)
        }
        assertBounds(first, RESIZED_WIDTH, RESIZED_HEIGHT)
        rule.runOnIdle { visible.value = false }
        rule.waitUntil(timeoutMillis = WINDOW_TIMEOUT_MS) {
            var disposed = false
            SwingUtilities.invokeAndWait { disposed = !first.isDisplayable }
            disposed
        }
        rule.runOnIdle { visible.value = true }
        rule.onNodeWithText(TITLE).assertExists()
        val reopened = frame()
        assertNotSame(first, reopened)
        assertBounds(reopened, RESIZED_WIDTH, RESIZED_HEIGHT)
    }

    private fun frame(): ComposeWindow {
        var frame: ComposeWindow? = null
        rule.waitUntil(timeoutMillis = WINDOW_TIMEOUT_MS) {
            SwingUtilities.invokeAndWait {
                frame = java.awt.Window.getWindows().filterIsInstance<ComposeWindow>().singleOrNull {
                    it.isShowing && it.title == TITLE && it === openedFrame.get()
                }
            }
            frame != null
        }
        return requireNotNull(frame)
    }

    private fun assertBounds(frame: ComposeWindow, width: Int, height: Int) {
        SwingUtilities.invokeAndWait {
            assertEquals(width, frame.width)
            assertEquals(height, frame.height)
        }
    }

    private companion object {
        const val TITLE = "Native initial size fixture"

        // Small enough to fit the 1024x768 displays of CI VMs, where the OS would clamp larger frames.
        const val INITIAL_WIDTH = 640
        const val INITIAL_HEIGHT = 480
        const val RESIZED_WIDTH = 720
        const val RESIZED_HEIGHT = 540
        const val WINDOW_TIMEOUT_MS = 5_000L
    }
}
