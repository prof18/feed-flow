package com.prof18.feedflow.desktop.startup

import com.prof18.feedflow.database.DesktopDatabaseFailure
import com.prof18.feedflow.database.DesktopDatabaseFailureKind
import com.prof18.feedflow.database.classifyDesktopDatabaseFailure

/**
 * Last line of defense for local database failures that escape a coroutine without containment.
 *
 * A lock is transient, so it is only reported. Storage, corruption and native-library failures make every
 * later read and write fail too, so they are reported and [onFatalFailure] swaps the main window for the
 * database error window. Anything else, including unexpected SQLite errors, goes to [next] as a crash.
 *
 * Install it before Sentry: Sentry wraps the current default handler and calls it after capturing, and its
 * `beforeSend` drops the fatal event for failures this handler contains (see [containedDatabaseFailure]).
 */
internal class DatabaseFailureUncaughtHandler(
    private val next: Thread.UncaughtExceptionHandler?,
    private val report: (Throwable) -> Unit,
    private val onFatalFailure: (DesktopDatabaseFailure) -> Unit,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        val failure = error.containedDatabaseFailure()
        if (failure == null) {
            forward(thread, error)
            return
        }
        report(error)
        if (failure.kind != DesktopDatabaseFailureKind.Locked) {
            onFatalFailure(failure)
        }
    }

    private fun forward(thread: Thread, error: Throwable) {
        if (next != null) {
            next.uncaughtException(thread, error)
        } else {
            // Same output as the JVM's own fallback, which can't be called: it would dispatch back here.
            System.err.print("Exception in thread \"${thread.name}\" ")
            error.printStackTrace(System.err)
        }
    }
}

/** The failure [DatabaseFailureUncaughtHandler] contains instead of crashing, or null when it is a real crash. */
internal fun Throwable.containedDatabaseFailure(): DesktopDatabaseFailure? =
    classifyDesktopDatabaseFailure()?.takeIf { it.kind != DesktopDatabaseFailureKind.Unexpected }
