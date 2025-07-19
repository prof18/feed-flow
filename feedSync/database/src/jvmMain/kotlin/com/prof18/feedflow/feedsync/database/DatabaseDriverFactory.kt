package com.prof18.feedflow.feedsync.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File
import java.util.Properties

internal fun createDatabaseDriver(
    appEnvironment: AppEnvironment,
    logger: Logger,
): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)

    val databaseName = if (appEnvironment.isDebug()) {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
    } else {
        SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD
    }
    val databasePath = File(appPath, "/$databaseName.db")

    // Configure SQLite properties for thread safety and corruption prevention
    val properties = Properties().apply {
        // Enable WAL mode for better concurrency and crash recovery
        setProperty("journal_mode", "WAL")
        // Set busy timeout to prevent immediate locking failures
        setProperty("busy_timeout", "30000")
        // Additional safety settings
        setProperty("synchronous", "NORMAL")
        setProperty("cache_size", "10000")
        setProperty("temp_store", "memory")
    }

    var driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}", properties)

    applyPragmaSettings(driver, logger)

    // Check database integrity and recover if needed
    driver = checkIntegrityAndRecover(
        originalDriver = driver, databasePath = databasePath, properties = properties, logger = logger,
    )

    val sqlCursor = driver.executeQuery(
        null,
        "PRAGMA user_version;",
        {
            QueryResult.Value(it.getLong(0))
        },
        0,
        null,
    )
    val currentVer: Long = sqlCursor.value ?: -1L

    if (currentVer == 0L) {
        FeedFlowFeedSyncDB.Schema.create(driver)
        setVersion(driver, FeedFlowFeedSyncDB.Schema.version)
        logger.d("init: created tables, setVersion to ${FeedFlowFeedSyncDB.Schema.version}")
    } else {
        val schemaVer = FeedFlowFeedSyncDB.Schema.version
        if (schemaVer > currentVer) {
            FeedFlowFeedSyncDB.Schema.migrate(driver, oldVersion = currentVer, newVersion = schemaVer)
            setVersion(driver, schemaVer)
            logger.d("init: migrated from $currentVer to $schemaVer")
        } else {
            logger.d("init with existing sync database")
        }
    }
    return driver
}

private fun checkIntegrityAndRecover(
    originalDriver: JdbcSqliteDriver,
    databasePath: File,
    properties: Properties,
    logger: Logger,
): JdbcSqliteDriver {
    try {
        val integrityCheck = originalDriver.executeQuery(
            null,
            "PRAGMA integrity_check;",
            { QueryResult.Value(it.getString(0)) },
            0,
            null,
        )
        if (integrityCheck.value != "ok") {
            logger.w("Database integrity check failed: ${integrityCheck.value}")
            logger.i("Attempting to recover corrupted sync database by recreating it")

            // Close the corrupted driver
            originalDriver.close()

            // Delete corrupted database files
            deleteDatabaseFiles(databasePath, logger)

            // Create a new driver with the same settings
            val newDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}", properties)
            applyPragmaSettings(newDriver, logger)

            logger.i("Successfully recovered sync database - will continue with normal initialization")
            return newDriver
        }
    } catch (e: Exception) {
        logger.w("Could not perform integrity check: ${e.message}")
    }
    return originalDriver
}

fun applyPragmaSettings(sqlDriver: SqlDriver, logger: Logger) {
    try {
        sqlDriver.execute(null, "PRAGMA journal_mode=WAL;", 0, null)
        sqlDriver.execute(null, "PRAGMA busy_timeout=30000;", 0, null)
        sqlDriver.execute(null, "PRAGMA foreign_keys=ON;", 0, null)
        sqlDriver.execute(null, "PRAGMA synchronous=NORMAL;", 0, null)
        sqlDriver.execute(null, "PRAGMA cache_size=10000;", 0, null)
        sqlDriver.execute(null, "PRAGMA temp_store=memory;", 0, null)
    } catch (e: Exception) {
        logger.w("Failed to apply some PRAGMA settings: ${e.message}")
    }
}

private fun deleteDatabaseFiles(databasePath: File, logger: Logger) {
    val dbFile = File(databasePath.absolutePath)
    val walFile = File("${databasePath.absolutePath}-wal")
    val shmFile = File("${databasePath.absolutePath}-shm")

    try {
        if (dbFile.exists()) {
            dbFile.delete()
            logger.d("Deleted corrupted database file")
        }
        if (walFile.exists()) {
            walFile.delete()
            logger.d("Deleted corrupted WAL file")
        }
        if (shmFile.exists()) {
            shmFile.delete()
            logger.d("Deleted corrupted SHM file")
        }
    } catch (deleteException: Exception) {
        logger.e("Failed to delete corrupted database files: ${deleteException.message}")
        throw deleteException
    }
}

fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}
