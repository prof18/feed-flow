package com.prof18.feedflow.feedsync.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.data.prepareSyncDatabaseSnapshot
import com.prof18.feedflow.feedsync.database.data.validateSyncDatabase
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File
import java.util.Properties

private val databaseInitLock = Any()

internal fun createDatabaseDriver(appEnvironment: AppEnvironment, logger: Logger): SqlDriver {
    val databaseName = if (appEnvironment.isDebug()) {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
    } else {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD
    }
    return openSyncDatabase(File(AppDataPathBuilder.getAppDataPath(appEnvironment), "$databaseName.db"), logger)
}

internal fun openSyncDatabase(databasePath: File, logger: Logger): SqlDriver = synchronized(databaseInitLock) {
    val exists = databasePath.exists()
    val url = "jdbc:sqlite:${databasePath.absolutePath}"
    if (exists) {
        check(databasePath.isFile) { "Sync database path is not a file" }
        // SQLITE_OPEN_READONLY avoids changing an existing file before it has been validated.
        val readOnly = Properties().apply { setProperty("open_mode", "1") }
        JdbcSqliteDriver(url, readOnly).use { validateSyncDatabase(it) }
    } else {
        databasePath.parentFile?.let {
            check(it.isDirectory || it.mkdirs()) { "Cannot create sync database directory" }
        }
    }

    val properties = Properties().apply {
        setProperty("journal_mode", "WAL")
        setProperty("busy_timeout", "30000")
        setProperty("synchronous", "NORMAL")
        setProperty("cache_size", "10000")
        setProperty("temp_store", "memory")
    }
    val driver = JdbcSqliteDriver(url, properties)
    try {
        if (exists) {
            prepareSyncDatabaseSnapshot(driver)
        } else {
            FeedFlowFeedSyncDB(driver).transaction {
                FeedFlowFeedSyncDB.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = ${FeedFlowFeedSyncDB.Schema.version}", 0)
            }
        }
        logger.d { "Sync database opened" }
        driver
    } catch (error: Exception) {
        try {
            driver.close()
        } catch (closeError: Exception) {
            error.addSuppressed(closeError)
        }
        throw error
    }
}
