package com.prof18.feedflow.database

import org.sqlite.NativeLibraryNotFoundException
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import java.sql.SQLException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DesktopDatabaseFailureTest {

    @Test
    fun `lock codes are classified as Locked`() {
        assertKind(DesktopDatabaseFailureKind.Locked, sqlite(SQLiteErrorCode.SQLITE_BUSY))
        assertKind(DesktopDatabaseFailureKind.Locked, sqlite(SQLiteErrorCode.SQLITE_BUSY_SNAPSHOT))
        assertKind(DesktopDatabaseFailureKind.Locked, sqlite(SQLiteErrorCode.SQLITE_LOCKED))
    }

    @Test
    fun `storage codes are classified as StorageUnavailable`() {
        listOf(
            SQLiteErrorCode.SQLITE_CANTOPEN,
            SQLiteErrorCode.SQLITE_IOERR_TRUNCATE,
            SQLiteErrorCode.SQLITE_IOERR_SHMSIZE,
            SQLiteErrorCode.SQLITE_FULL,
            SQLiteErrorCode.SQLITE_READONLY,
            SQLiteErrorCode.SQLITE_PERM,
        ).forEach { code ->
            assertKind(DesktopDatabaseFailureKind.StorageUnavailable, sqlite(code))
        }
    }

    @Test
    fun `corruption is classified as Corrupt`() {
        assertKind(DesktopDatabaseFailureKind.Corrupt, sqlite(SQLiteErrorCode.SQLITE_CORRUPT))
        assertKind(DesktopDatabaseFailureKind.Corrupt, sqlite(SQLiteErrorCode.SQLITE_NOTADB))
        assertKind(DesktopDatabaseFailureKind.Corrupt, InvalidDatabaseSchemaException("missing table"))
    }

    @Test
    fun `native library failures are classified as NativeLibrary`() {
        assertKind(DesktopDatabaseFailureKind.NativeLibrary, UnsatisfiedLinkError("_open_utf8"))
        assertKind(
            DesktopDatabaseFailureKind.NativeLibrary,
            SQLException("Error opening connection", NativeLibraryNotFoundException("no native library")),
        )
    }

    @Test
    fun `other SQLite codes are Unexpected`() {
        assertKind(DesktopDatabaseFailureKind.Unexpected, sqlite(SQLiteErrorCode.SQLITE_ERROR))
    }

    @Test
    fun `extended codes keep their extended name`() {
        val failure = sqlite(SQLiteErrorCode.SQLITE_IOERR_TRUNCATE).classifyDesktopDatabaseFailure()

        assertEquals("SQLITE_IOERR_TRUNCATE", failure?.sqliteResultCode)
    }

    @Test
    fun `wrapped failures keep their kind`() {
        val inputs = listOf(
            sqlite(SQLiteErrorCode.SQLITE_BUSY) to DesktopDatabaseFailureKind.Locked,
            sqlite(SQLiteErrorCode.SQLITE_FULL) to DesktopDatabaseFailureKind.StorageUnavailable,
            sqlite(SQLiteErrorCode.SQLITE_CORRUPT) to DesktopDatabaseFailureKind.Corrupt,
            UnsatisfiedLinkError("_open_utf8") to DesktopDatabaseFailureKind.NativeLibrary,
            sqlite(SQLiteErrorCode.SQLITE_ERROR) to DesktopDatabaseFailureKind.Unexpected,
        )

        inputs.forEach { (error, kind) ->
            assertKind(kind, RuntimeException("outer", RuntimeException("inner", error)))
        }
    }

    @Test
    fun `non database failures are not classified`() {
        assertNull(IllegalStateException("boom").classifyDesktopDatabaseFailure())
        assertNull(CancellationException("cancelled").classifyDesktopDatabaseFailure())
    }

    @Test
    fun `cancellation anywhere in the chain is not classified`() {
        val error = CancellationException("cancelled").apply { initCause(sqlite(SQLiteErrorCode.SQLITE_BUSY)) }

        assertNull(error.classifyDesktopDatabaseFailure())
    }

    @Test
    fun `startup exception is found in the cause chain`() {
        val startup = DesktopDatabaseStartupException(
            failure = DesktopDatabaseFailure(DesktopDatabaseFailureKind.Locked, "SQLITE_BUSY"),
            cause = sqlite(SQLiteErrorCode.SQLITE_BUSY),
        )

        assertSame(startup, RuntimeException("koin", startup).findDesktopDatabaseStartupException())
        assertNull(IllegalStateException("boom").findDesktopDatabaseStartupException())
    }

    private fun sqlite(code: SQLiteErrorCode) = SQLiteException("failure", code)

    private fun assertKind(expected: DesktopDatabaseFailureKind, error: Throwable) {
        assertEquals(expected, error.classifyDesktopDatabaseFailure()?.kind, "for $error")
    }
}
