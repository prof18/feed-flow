package com.prof18.feedflow.database

/**
 * True when this is a failure of the local database itself (locked, storage, corruption, native library).
 * Only the desktop recognizes them: Android and iOS return false, so callers keep today's behavior there.
 */
expect fun Throwable.isDatabaseFailure(): Boolean

/**
 * A database failure inside a [DatabaseHelper] transaction, carrying how long the transaction ran:
 * close to the busy timeout means another process held the lock, close to zero means SQLite gave up
 * without waiting.
 */
class DatabaseTransactionException(
    val label: String,
    val elapsedMs: Long,
    cause: Throwable,
) : RuntimeException("Database transaction failed ($label) after $elapsedMs ms", cause)
