package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.NO_VERSION_CHECK
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

fun prepareSyncDatabaseFile(url: NSURL) {
    val driver = NativeSqliteDriver(
        DatabaseConfiguration(
            name = requireNotNull(url.lastPathComponent),
            version = NO_VERSION_CHECK,
            create = { error("Downloaded sync database must already exist") },
            journalMode = JournalMode.DELETE,
            extendedConfig = DatabaseConfiguration.Extended(
                basePath = requireNotNull(url.URLByDeletingLastPathComponent?.path),
            ),
        ),
    )
    try {
        prepareSyncDatabaseSnapshot(driver)
    } finally {
        driver.close()
    }
}

/**
 * Removes the WAL, shared-memory and rollback journal files of a closed sync database before its main file
 * is replaced. SQLite would otherwise replay the old database's journal onto the new file and corrupt it.
 */
@OptIn(ExperimentalForeignApi::class)
fun removeSyncDatabaseJournals(databaseUrl: NSURL) {
    val fileManager = NSFileManager.defaultManager
    val databasePath = requireNotNull(databaseUrl.path)
    listOf("-wal", "-shm", "-journal").forEach { suffix ->
        val path = "$databasePath$suffix"
        if (fileManager.fileExistsAtPath(path)) {
            check(fileManager.removeItemAtPath(path, null)) { "Cannot remove the sync database $suffix file" }
        }
    }
}
