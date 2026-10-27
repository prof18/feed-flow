package com.prof18.feedflow.database

import org.sqlite.NativeLibraryNotFoundException
import org.sqlite.SQLiteErrorCode
import org.sqlite.SQLiteException
import kotlin.coroutines.cancellation.CancellationException

enum class DesktopDatabaseFailureKind { Locked, StorageUnavailable, Corrupt, NativeLibrary, Unexpected }

data class DesktopDatabaseFailure(
    val kind: DesktopDatabaseFailureKind,
    val sqliteResultCode: String?,
)

class InvalidDatabaseSchemaException(message: String) : RuntimeException(message)

class DesktopDatabaseStartupException(
    val failure: DesktopDatabaseFailure,
    cause: Throwable,
) : RuntimeException(cause)

fun Throwable.classifyDesktopDatabaseFailure(): DesktopDatabaseFailure? {
    for (error in causeChain()) {
        when (error) {
            is CancellationException -> return null
            is SQLiteException -> return error.toFailure()
            is InvalidDatabaseSchemaException ->
                return DesktopDatabaseFailure(DesktopDatabaseFailureKind.Corrupt, sqliteResultCode = null)
            is NativeLibraryNotFoundException, is UnsatisfiedLinkError ->
                return DesktopDatabaseFailure(DesktopDatabaseFailureKind.NativeLibrary, sqliteResultCode = null)
        }
    }
    return null
}

fun Throwable.findDesktopDatabaseStartupException(): DesktopDatabaseStartupException? =
    causeChain().filterIsInstance<DesktopDatabaseStartupException>().firstOrNull()

private fun SQLiteException.toFailure(): DesktopDatabaseFailure {
    val primaryCode = SQLiteErrorCode.getErrorCode(resultCode.code and PRIMARY_CODE_MASK)
    val kind = when (primaryCode) {
        SQLiteErrorCode.SQLITE_BUSY,
        SQLiteErrorCode.SQLITE_LOCKED,
        -> DesktopDatabaseFailureKind.Locked

        SQLiteErrorCode.SQLITE_CANTOPEN,
        SQLiteErrorCode.SQLITE_IOERR,
        SQLiteErrorCode.SQLITE_FULL,
        SQLiteErrorCode.SQLITE_READONLY,
        SQLiteErrorCode.SQLITE_PERM,
        -> DesktopDatabaseFailureKind.StorageUnavailable

        SQLiteErrorCode.SQLITE_CORRUPT,
        SQLiteErrorCode.SQLITE_NOTADB,
        -> DesktopDatabaseFailureKind.Corrupt

        else -> DesktopDatabaseFailureKind.Unexpected
    }
    return DesktopDatabaseFailure(kind, sqliteResultCode = resultCode.name)
}

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH)

private const val PRIMARY_CODE_MASK = 0xFF
private const val MAX_CAUSE_DEPTH = 10
