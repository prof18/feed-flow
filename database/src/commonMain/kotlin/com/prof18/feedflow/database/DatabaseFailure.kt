package com.prof18.feedflow.database

/**
 * True when this is a failure of the local database itself (locked, storage, corruption, native library).
 * Only the desktop recognizes them: Android and iOS return false, so callers keep today's behavior there.
 */
expect fun Throwable.isDatabaseFailure(): Boolean
