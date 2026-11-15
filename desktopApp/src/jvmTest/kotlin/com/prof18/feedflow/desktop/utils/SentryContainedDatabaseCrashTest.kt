package com.prof18.feedflow.desktop.utils

import com.prof18.feedflow.database.DatabaseTransactionException
import io.sentry.EventProcessor
import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class SentryContainedDatabaseCrashTest {

    @Test
    fun `the fatal copy of a contained storage failure is dropped`() {
        val event = event(storageFailure(), SentryLevel.FATAL)

        assertNull(dropContainedDatabaseCrash(event))
    }

    @Test
    fun `the handled report of a contained storage failure is kept`() {
        val event = event(storageFailure(), SentryLevel.ERROR)

        assertSame(event, dropContainedDatabaseCrash(event))
    }

    @Test
    fun `other crashes are kept`() {
        val event = event(IllegalStateException("bug"), SentryLevel.FATAL)

        assertSame(event, dropContainedDatabaseCrash(event))
    }

    @Test
    fun `the handled report survives duplicate detection after the fatal copy is dropped`() {
        val options = SentryOptions().apply { dropContainedDatabaseCrashes() }
        val error = storageFailure()

        assertNull(options.process(event(error, SentryLevel.FATAL)))
        assertNotNull(options.process(event(error, SentryLevel.ERROR)))
    }

    // Runs the event through the options' processors in order, as SentryClient does.
    private fun SentryOptions.process(event: SentryEvent): SentryEvent? =
        eventProcessors.fold<EventProcessor, SentryEvent?>(event) { current, processor ->
            current?.let { processor.process(it, Hint()) }
        }

    private fun event(error: Throwable, level: SentryLevel) = SentryEvent(error).apply { this.level = level }

    private fun storageFailure() = DatabaseTransactionException(
        label = "unnamed",
        elapsedMs = 54,
        cause = SQLiteException("[SQLITE_CANTOPEN]", SQLiteErrorCode.SQLITE_CANTOPEN),
    )
}
