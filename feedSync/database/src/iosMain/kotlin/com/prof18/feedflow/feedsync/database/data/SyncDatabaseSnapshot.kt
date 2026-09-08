package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode
import co.touchlab.sqliter.NO_VERSION_CHECK
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
