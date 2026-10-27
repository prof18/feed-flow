package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.Logger
import com.prof18.feedflow.database.DatabaseTransactionException
import com.prof18.feedflow.database.classifyDesktopDatabaseFailure
import com.prof18.feedflow.database.findDesktopDatabaseStartupException
import io.sentry.Sentry
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Process-wide values the reporter attaches to every event; set by the desktop app at startup. */
object DatabaseFailureReporterConfig {
    @Volatile
    var singleInstanceLockState: String = "unknown"
}

internal class DatabaseFailureReporter(
    private val isSentryEnabled: () -> Boolean = { Sentry.isEnabled() },
    private val capture: (Throwable, Map<String, String>) -> Unit = { error, tags ->
        Sentry.captureException(error) { scope -> tags.forEach { (key, value) -> scope.setTag(key, value) } }
    },
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val singleInstanceLockState: () -> String = { DatabaseFailureReporterConfig.singleInstanceLockState },
) {
    private val lastCaptures = ConcurrentHashMap<String, TimeMark>()

    fun report(operation: String, error: Throwable, logger: Logger) {
        // Startup decides the kind itself (e.g. a failed quarantine move is a plain IOException).
        val failure = error.findDesktopDatabaseStartupException()?.failure
            ?: error.classifyDesktopDatabaseFailure()
        val waitMs = generateSequence(error) { it.cause }.take(MAX_CAUSE_DEPTH)
            .filterIsInstance<DatabaseTransactionException>()
            .firstOrNull()
            ?.elapsedMs
        // Debug level on purpose: SentryLogWriter captures Warn and above, which would duplicate the event.
        logger.d(error) { "Database failure in $operation: ${failure?.kind} ${failure?.sqliteResultCode}" }
        if (!isSentryEnabled()) return

        // Read-status writes run on every scroll; a lock that persists must not send one event per scroll.
        val key = "$operation:${failure?.sqliteResultCode}"
        val now = timeSource.markNow()
        val previous = lastCaptures[key]
        if (previous != null && previous.elapsedNow() < REPORT_DEDUPE) return
        lastCaptures[key] = now

        capture(
            error,
            mapOf(
                "database.operation" to operation,
                "database.failure_kind" to (failure?.kind?.name ?: UNKNOWN),
                "database.sqlite_result_code" to (failure?.sqliteResultCode ?: UNKNOWN),
                "database.wait_ms" to (waitMs?.let { "${it / WAIT_ROUNDING_MS * WAIT_ROUNDING_MS}" } ?: UNKNOWN),
                "single_instance_lock" to singleInstanceLockState(),
            ),
        )
    }

    private companion object {
        val REPORT_DEDUPE = 10.minutes
        const val WAIT_ROUNDING_MS = 100
        const val MAX_CAUSE_DEPTH = 10
        const val UNKNOWN = "unknown"
    }
}

private val defaultReporter = DatabaseFailureReporter()

internal actual fun reportDatabaseFailure(operation: String, error: Throwable, logger: Logger) {
    defaultReporter.report(operation, error, logger)
}

fun reportDesktopDatabaseFailure(operation: String, error: Throwable) {
    defaultReporter.report(operation, error, Logger.withTag("DatabaseFailure"))
}
