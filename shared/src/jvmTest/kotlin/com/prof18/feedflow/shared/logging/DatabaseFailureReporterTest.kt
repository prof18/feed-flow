package com.prof18.feedflow.shared.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource

class DatabaseFailureReporterTest {

    private val captures = mutableListOf<Pair<Throwable, Map<String, String>>>()
    private val logSeverities = mutableListOf<Severity>()
    private val timeSource = TestTimeSource()
    private val logger = Logger(
        loggerConfigInit(
            object : LogWriter() {
                override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
                    logSeverities += severity
                }
            },
            minSeverity = Severity.Verbose,
        ),
        tag = "Test",
    )

    private fun reporter(lockState: String = "held") = DatabaseFailureReporter(
        isSentryEnabled = { true },
        capture = { error, tags -> captures += error to tags },
        timeSource = timeSource,
        singleInstanceLockState = { lockState },
    )

    @Test
    fun `one failure produces one capture with all tags`() {
        val error = SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY)

        reporter().report("refresh_write", error, logger)

        assertEquals(1, captures.size)
        val (captured, tags) = captures.single()
        assertEquals(error, captured)
        assertEquals("refresh_write", tags["database.operation"])
        assertEquals("Locked", tags["database.failure_kind"])
        assertEquals("SQLITE_BUSY", tags["database.sqlite_result_code"])
        assertEquals("unknown", tags["database.wait_ms"])
        assertEquals("held", tags["single_instance_lock"])
    }

    @Test
    fun `the log line stays below the level SentryLogWriter captures`() {
        reporter().report("refresh_write", SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY), logger)

        assertTrue(logSeverities.isNotEmpty())
        assertTrue(logSeverities.all { it < Severity.Warn }, "severities: $logSeverities")
    }

    @Test
    fun `a repeat within the dedupe window is not captured again`() {
        val reporter = reporter()
        val error = SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY)

        reporter.report("user_write", error, logger)
        timeSource += 5.minutes
        reporter.report("user_write", error, logger)

        assertEquals(1, captures.size)

        timeSource += 6.minutes
        reporter.report("user_write", error, logger)

        assertEquals(2, captures.size)
    }

    @Test
    fun `different operations or codes are captured separately`() {
        val reporter = reporter()

        reporter.report("user_write", SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY), logger)
        reporter.report("refresh_write", SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY), logger)
        reporter.report("user_write", SQLiteException("full", SQLiteErrorCode.SQLITE_FULL), logger)

        assertEquals(3, captures.size)
    }

    @Test
    fun `nothing is captured when Sentry is disabled`() {
        DatabaseFailureReporter(
            isSentryEnabled = { false },
            capture = { error, tags -> captures += error to tags },
            timeSource = timeSource,
            singleInstanceLockState = { "held" },
        ).report("user_write", SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY), logger)

        assertTrue(captures.isEmpty())
    }
}
