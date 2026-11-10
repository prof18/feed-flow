package com.prof18.feedflow.desktop.e2e.harness

import dev.sebastiano.spectre.agent.launch.LaunchedSession
import java.io.File
import kotlin.concurrent.thread

/** Failure evidence: the tree, the windows, a capture per window and the app's logs. */
internal object Evidence {

    fun dir(testClass: String, method: String): File =
        File(EVIDENCE_DIR, "$testClass/$method").apply { mkdirs() }

    fun collect(session: LaunchedSession?, dir: File) {
        val errors = StringBuffer()
        val worker = thread(name = "desktop-e2e-evidence", isDaemon = true) {
            fun item(name: String, block: () -> Unit) {
                runCatching(block).onFailure { errors.append("$name: ${it.stackTraceToString()}\n") }
            }
            val automator = session?.automator
            if (automator != null) {
                item("tree") { File(dir, "tree.txt").writeText(automator.printTree()) }
                val windows = runCatching { automator.windows() }.getOrDefault(emptyList())
                item("windows") { File(dir, "windows.txt").writeText(windows.joinToString("\n")) }
                windows.filter { it.isShowing }.forEach { window ->
                    item("capture-${window.index}") {
                        val capture = automator.capture(window.index)
                        File(dir, "capture-${window.index}.png").writeBytes(capture.pngBytes)
                        File(dir, "capture-${window.index}.json").writeText(capture.captureJson)
                    }
                }
            }
            if (session != null) {
                item("stdout") { session.stdoutPath.toFile().copyTo(File(dir, "app-stdout.log"), overwrite = true) }
                item("stderr") { session.stderrPath.toFile().copyTo(File(dir, "app-stderr.log"), overwrite = true) }
            }
        }
        worker.join(EVIDENCE_TIMEOUT_MS)
        if (worker.isAlive) errors.append("evidence collection timed out after $EVIDENCE_TIMEOUT_MS ms\n")
        if (errors.isNotEmpty()) File(dir, "evidence-errors.txt").writeText(errors.toString())
    }
}
