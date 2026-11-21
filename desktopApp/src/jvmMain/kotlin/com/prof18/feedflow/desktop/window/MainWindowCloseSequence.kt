package com.prof18.feedflow.desktop.window

/**
 * Closes the main window in order: save its geometry while it is still showing, run the app cleanup,
 * then exit. Only the first close request runs it; later ones, such as a second click on the close
 * button while the backup runs, do nothing. Exit runs even when a step fails or is cancelled.
 */
internal class MainWindowCloseSequence(
    private val saveWindowState: suspend () -> Unit,
    private val cleanup: suspend () -> Unit,
    private val exit: () -> Unit,
) {
    private var started = false

    /** Call it from the UI thread. */
    suspend fun run() {
        if (started) return
        started = true
        try {
            saveWindowState()
            cleanup()
        } finally {
            exit()
        }
    }
}
