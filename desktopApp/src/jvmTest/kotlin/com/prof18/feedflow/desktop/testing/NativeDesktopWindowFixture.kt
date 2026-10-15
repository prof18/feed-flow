package com.prof18.feedflow.desktop.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import java.awt.Window as AwtWindow

internal class NativeDesktopWindowFixture(
    private val content: @Composable () -> Unit,
) {
    private val windowRef = AtomicReference<AwtWindow?>()
    private val exitRef = AtomicReference<(() -> Unit)?>(null)
    private lateinit var appThread: Thread

    fun start() {
        appThread = Thread({
            application(exitProcessOnExit = false) {
                exitRef.set(::exitApplication)
                Window(
                    onCloseRequest = ::exitApplication,
                    title = WINDOW_TITLE,
                    state = WindowState(size = DpSize(WINDOW_WIDTH.dp, WINDOW_HEIGHT.dp)),
                ) {
                    windowRef.compareAndSet(null, window)
                    content()
                }
            }
        }, "feedflow-native-test-window").apply {
            isDaemon = true
            start()
        }
    }

    fun awaitWindow(timeoutMillis: Long = WINDOW_START_TIMEOUT_MS): AwtWindow {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            windowRef.get()?.let { window ->
                var showing = false
                SwingUtilities.invokeAndWait { showing = window.isShowing }
                if (showing) return window
            }
            Thread.sleep(WINDOW_POLL_INTERVAL_MS)
        }
        error("Native test window did not start within $timeoutMillis ms")
    }

    fun close() {
        SwingUtilities.invokeAndWait { exitRef.get()?.invoke() }
        appThread.join(WINDOW_STOP_TIMEOUT_MS)
    }

    companion object {
        private const val WINDOW_TITLE = "FeedFlow native test"
        private const val WINDOW_WIDTH = 560
        private const val WINDOW_HEIGHT = 640
        private const val WINDOW_START_TIMEOUT_MS = 10_000L
        private const val WINDOW_POLL_INTERVAL_MS = 25L
        private const val WINDOW_STOP_TIMEOUT_MS = 2_000L
    }
}

internal class NativeDesktopWindowFixtureRule(
    private val fixture: NativeDesktopWindowFixture,
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            fixture.start()
            try {
                base.evaluate()
            } finally {
                fixture.close()
            }
        }
    }
}
