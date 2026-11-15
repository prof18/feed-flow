package com.prof18.feedflow.desktop.startup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.database.DatabaseTransactionException
import com.prof18.feedflow.database.DesktopDatabaseFailure
import com.prof18.feedflow.database.DesktopDatabaseFailureKind
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseFailureUncaughtHandlerTest {

    private val reported = mutableListOf<Throwable>()
    private val fatalFailures = mutableListOf<DesktopDatabaseFailure>()
    private val forwarded = mutableListOf<Throwable>()

    private val handler = DatabaseFailureUncaughtHandler(
        next = { _, error -> forwarded += error },
        report = { reported += it },
        onFatalFailure = { fatalFailures += it },
    )

    @Test
    fun `a storage failure escaping a transaction is reported and opens the database error window`() {
        val error = transactionFailure(SQLiteErrorCode.SQLITE_CANTOPEN)

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf<Throwable>(error), reported)
        assertEquals(
            listOf(DesktopDatabaseFailure(DesktopDatabaseFailureKind.StorageUnavailable, "SQLITE_CANTOPEN")),
            fatalFailures,
        )
        assertTrue(forwarded.isEmpty())
    }

    @Test
    fun `a corrupt database is reported and opens the database error window`() {
        val error = transactionFailure(SQLiteErrorCode.SQLITE_CORRUPT)

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf<Throwable>(error), reported)
        assertEquals(DesktopDatabaseFailureKind.Corrupt, fatalFailures.single().kind)
        assertTrue(forwarded.isEmpty())
    }

    @Test
    fun `a lock failure is only reported because it is transient`() {
        val error = transactionFailure(SQLiteErrorCode.SQLITE_BUSY)

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf<Throwable>(error), reported)
        assertTrue(fatalFailures.isEmpty())
        assertTrue(forwarded.isEmpty())
    }

    @Test
    fun `an unexpected SQLite error is forwarded as a crash`() {
        val error = transactionFailure(SQLiteErrorCode.SQLITE_CONSTRAINT)

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf<Throwable>(error), forwarded)
        assertTrue(reported.isEmpty())
        assertTrue(fatalFailures.isEmpty())
    }

    @Test
    fun `a non-database error is forwarded as a crash`() {
        val error = IllegalStateException("bug")

        handler.uncaughtException(Thread.currentThread(), error)

        assertEquals(listOf<Throwable>(error), forwarded)
        assertTrue(reported.isEmpty())
        assertTrue(fatalFailures.isEmpty())
    }

    @Test
    fun `without a next handler a non-database error does not throw`() {
        val handler = DatabaseFailureUncaughtHandler(next = null, report = {}, onFatalFailure = {})

        handler.uncaughtException(Thread.currentThread(), IllegalStateException("bug"))
    }

    @Test
    fun `only contained database failures are recognized for the crash report filter`() {
        assertEquals(
            DesktopDatabaseFailureKind.StorageUnavailable,
            transactionFailure(SQLiteErrorCode.SQLITE_CANTOPEN).containedDatabaseFailure()?.kind,
        )
        assertEquals(
            DesktopDatabaseFailureKind.Locked,
            transactionFailure(SQLiteErrorCode.SQLITE_BUSY).containedDatabaseFailure()?.kind,
        )
        assertNull(transactionFailure(SQLiteErrorCode.SQLITE_CONSTRAINT).containedDatabaseFailure())
        assertNull(IllegalStateException("bug").containedDatabaseFailure())
    }

    @Test
    fun `a write from a coroutine after the data folder disappears reaches the handler as storage loss`() {
        val dataFolder = Files.createTempDirectory("feedflow-db").toFile()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${File(dataFolder, "FeedFlow.db").absolutePath}")
        driver.execute(null, "CREATE TABLE item (id INTEGER)", 0)
        dataFolder.deleteRecursively()

        val handled = CountDownLatch(1)
        val handler = DatabaseFailureUncaughtHandler(
            next = { _, error -> forwarded += error },
            report = { reported += it },
            onFatalFailure = {
                fatalFailures += it
                handled.countDown()
            },
        )
        // A fresh thread gets a new connection from the driver, which is what failed in the field. A plain
        // thread instead of a coroutine: kotlinx-coroutines-test collects uncaught coroutine exceptions
        // globally and would fail the next runTest with this one.
        val writer = Thread { driver.execute(null, "INSERT INTO item VALUES (1)", 0) }
        writer.uncaughtExceptionHandler = handler
        try {
            writer.start()
            assertTrue(handled.await(10, TimeUnit.SECONDS))
        } finally {
            writer.join()
            driver.close()
        }

        assertEquals(DesktopDatabaseFailureKind.StorageUnavailable, fatalFailures.single().kind)
        assertEquals("SQLITE_CANTOPEN", fatalFailures.single().sqliteResultCode)
        assertTrue(forwarded.isEmpty())
    }

    private fun transactionFailure(code: SQLiteErrorCode) = DatabaseTransactionException(
        label = "unnamed",
        elapsedMs = 54,
        cause = SQLiteException("[${code.name}]", code),
    )
}
