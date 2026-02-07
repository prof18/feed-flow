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

private val databaseInitLock = Any()
private val syncSchemaTables = listOf(
    "synced_feed_item",
    "synced_feed_source",
    "synced_feed_source_category",
    "sync_metadata",
)

internal fun createDatabaseDriver(
    appEnvironment: AppEnvironment,
    logger: Logger,
): SqlDriver = synchronized(databaseInitLock) {
    val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)
    val schemaVersion = FeedFlowFeedSyncDB.Schema.version

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

    var driver: JdbcSqliteDriver? = null
    try {
        driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}", properties)

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
            if (hasAnySyncSchemaTables(driver)) {
                logger.w("init: existing sync tables with user_version 0, recreating schema")
                recreateSyncSchema(driver, schemaVersion)
            } else {
                FeedFlowFeedSyncDB.Schema.create(driver)
                setVersion(driver, schemaVersion)
                logger.d("init: created tables, setVersion to $schemaVersion")
            }
        } else {
            if (schemaVersion > currentVer) {
                FeedFlowFeedSyncDB.Schema.migrate(driver, oldVersion = currentVer, newVersion = schemaVersion)
                setVersion(driver, schemaVersion)
                logger.d("init: migrated from $currentVer to $schemaVersion")
            } else {
                logger.d("init with existing sync database")
            }
        }

        if (!hasAllSyncSchemaTables(driver)) {
            throw java.sql.SQLException("sync database schema is incomplete")
        }

        return driver
    } catch (_: java.sql.SQLException) {
        driver?.close()
        deleteDatabaseFiles(databasePath, logger)
        val newDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}", properties)
        applyPragmaSettings(newDriver, logger)
        recreateSyncSchema(newDriver, schemaVersion)
        return newDriver
    }
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
            // Close the corrupted driver
            originalDriver.close()

            // Delete corrupted database files
            deleteDatabaseFiles(databasePath, logger)

            // Create a new driver with the same settings
            val newDriver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}", properties)
            applyPragmaSettings(newDriver, logger)

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

private fun hasAnySyncSchemaTables(driver: SqlDriver): Boolean {
    val tableCount = countSyncTables(driver)
    return tableCount > 0
}

private fun hasAllSyncSchemaTables(driver: SqlDriver): Boolean {
    val tableCount = countSyncTables(driver)
    return tableCount == syncSchemaTables.size.toLong()
}

private fun countSyncTables(driver: SqlDriver): Long {
    val tableNames = syncSchemaTables.joinToString(",") { "'$it'" }
    val checkTableCursor = driver.executeQuery(
        null,
        "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ($tableNames);",
        {
            QueryResult.Value(it.getLong(0))
        },
        0,
        null,
    )
    return checkTableCursor.value ?: 0L
}

private fun recreateSyncSchema(driver: SqlDriver, version: Long) {
    syncSchemaTables.forEach { tableName ->
        driver.execute(null, "DROP TABLE IF EXISTS $tableName;", 0, null)
    }
    FeedFlowFeedSyncDB.Schema.create(driver)
    setVersion(driver, version)
}
