package com.prof18.feedflow.desktop.startup

import com.prof18.feedflow.i18n.EnFeedFlowStrings
import com.prof18.feedflow.i18n.FeedFlowStrings
import com.prof18.feedflow.i18n.feedFlowStrings
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.io.File
import java.time.Instant
import java.util.Locale
import java.util.Properties
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.UIManager

internal const val STARTUP_CRASH_LOG_FILE_NAME = "startup-error.log"

private const val DETAILS_WIDTH = 560
private const val DETAILS_HEIGHT = 260

/**
 * Records a crash that stops FeedFlow before its window opens. Without this the packaged launcher only
 * shows a generic "Failed to launch JVM" dialog and the cause is lost.
 */
internal fun handleStartupCrash(
    error: Throwable,
    appVersion: String?,
    dataFolder: File?,
) {
    val report = startupCrashReportText(error = error, appVersion = appVersion)
    System.err.println(report)
    val logFile = writeStartupCrashReport(
        report = report,
        folders = listOfNotNull(dataFolder, File(System.getProperty("java.io.tmpdir"))),
    )
    showStartupCrashDialog(strings = startupStrings(Locale.getDefault()), logFile = logFile, report = report)
}

internal fun startupCrashReportText(
    error: Throwable,
    appVersion: String?,
    systemProperties: Properties = System.getProperties(),
    now: Instant = Instant.now(),
): String {
    val home = systemProperties.getProperty("user.home")
    fun property(name: String): String = systemProperties.getProperty(name) ?: "unknown"

    val report = buildString {
        appendLine("FeedFlow startup error")
        appendLine("Time: $now")
        appendLine("Version: ${appVersion ?: "unknown"}")
        appendLine("OS: ${property("os.name")} ${property("os.version")} (${property("os.arch")})")
        appendLine("Java: ${property("java.runtime.version")} (${property("java.vendor")})")
        appendLine("Java home: ${property("java.home")}")
        appendLine("Working directory: ${property("user.dir")}")
        appendLine()
        append(error.stackTraceToString())
    }
    // Reports get pasted into public issues; keep the user's account name out of them.
    return if (home.isNullOrBlank()) report else report.replace(home, "~")
}

/** Writes [report] into the first folder that accepts it. Never throws. */
internal fun writeStartupCrashReport(report: String, folders: List<File>): File? {
    for (folder in folders) {
        try {
            folder.mkdirs()
            val file = File(folder, STARTUP_CRASH_LOG_FILE_NAME)
            file.writeText(report)
            return file
        } catch (_: Exception) {
            // Try the next folder.
        }
    }
    return null
}

internal fun startupStrings(locale: Locale): FeedFlowStrings {
    val country = locale.country.takeIf { it.isNotBlank() }
    val candidates = listOfNotNull(
        country?.let { "${locale.language}_$it" },
        locale.language,
    )
    return candidates.firstNotNullOfOrNull { feedFlowStrings[it] } ?: EnFeedFlowStrings
}

// Plain Swing on purpose: the crash may come from Compose or its native renderer, so the dialog
// can't depend on them.
private fun showStartupCrashDialog(strings: FeedFlowStrings, logFile: File?, report: String) {
    if (GraphicsEnvironment.isHeadless()) return

    val show: () -> Unit = {
        try {
            val message = if (logFile != null) {
                strings.desktopStartupCrash(logFile.absolutePath)
            } else {
                strings.desktopStartupCrashNoLog
            }
            // A wrapping, read-only text area keeps a long log path from stretching the dialog.
            val messageArea = JTextArea(message).apply {
                isEditable = false
                isOpaque = false
                lineWrap = true
                wrapStyleWord = true
                font = UIManager.getFont("OptionPane.messageFont") ?: font
                setSize(DETAILS_WIDTH, Short.MAX_VALUE.toInt())
            }
            val details = JTextArea(report).apply {
                isEditable = false
                caretPosition = 0
            }
            val scrollPane = JScrollPane(details).apply {
                preferredSize = Dimension(DETAILS_WIDTH, DETAILS_HEIGHT)
            }
            JOptionPane.showOptionDialog(
                null,
                arrayOf<Any>(messageArea, scrollPane),
                strings.appName,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                arrayOf(strings.databaseQuit),
                strings.databaseQuit,
            )
        } catch (_: Exception) {
            // The report is already on stderr and, when possible, on disk.
        }
    }
    if (SwingUtilities.isEventDispatchThread()) {
        show()
    } else {
        try {
            SwingUtilities.invokeAndWait(show)
        } catch (_: Exception) {
            // Same as above: nothing else to try.
        }
    }
}
