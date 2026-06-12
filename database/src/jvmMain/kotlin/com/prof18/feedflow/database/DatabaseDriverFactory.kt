package com.prof18.feedflow.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DesktopDatabaseErrorState
import com.prof18.feedflow.db.FeedFlowDB
import java.io.File
import java.sql.SQLException
import java.util.Properties

fun createDatabaseDriver(
    appEnvironment: AppEnvironment,
    logger: Logger,
): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)

    val databasePath = File(appPath, "/${DatabaseHelper.DB_FILE_NAME_WITH_EXTENSION}")

    fun createDriver(): JdbcSqliteDriver {
        databasePath.parentFile?.mkdirs()
        val properties = Properties().apply {
            setProperty("journal_mode", "WAL")
            setProperty("synchronous", "NORMAL")
            setProperty("busy_timeout", "30000")
        }
        return JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + databasePath.absolutePath, properties)
    }

    var driver = createDriver()

    try {
        return initDatabase(driver, logger)
    } catch (_: SQLException) {
        DesktopDatabaseErrorState.setError(true)
        driver.close()
        deleteDatabaseFiles(databasePath, logger)
        driver = createDriver()
        return initDatabase(driver, logger)
    }
}

private fun initDatabase(driver: SqlDriver, logger: Logger): SqlDriver {
    val schemaVersion = FeedFlowDB.Schema.version

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
        if (hasAnyUserTables(driver)) {
            logger.w("init: existing tables with user_version 0, recreating schema")
            DesktopDatabaseErrorState.setError(true)
            dropAllUserObjects(driver)
        }
        FeedFlowDB.Schema.create(driver)
        setVersion(driver, schemaVersion)
        logger.d("init: created tables, setVersion to $schemaVersion")
    } else {
        if (schemaVersion > currentVer) {
            FeedFlowDB.Schema.migrate(driver, oldVersion = currentVer, newVersion = schemaVersion)
            setVersion(driver, schemaVersion)
            logger.d("init: migrated from $currentVer to $schemaVersion")
        } else {
            logger.d("init with existing database")
        }
    }
    validateRequiredSchemaObjects(driver)
    return driver
}

fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}

private fun hasAnyUserTables(driver: SqlDriver): Boolean {
    val cursor = driver.executeQuery(
        null,
        "SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';",
        {
            QueryResult.Value(it.getLong(0))
        },
        0,
        null,
    )
    return (cursor.value ?: 0L) > 0L
}

internal fun validateRequiredSchemaObjects(driver: SqlDriver) {
    val missingObjects = buildList {
        requiredTables.filterNot { driver.hasSchemaObject(type = "table", name = it) }
            .mapTo(this) { "table:$it" }
        requiredViews.filterNot { driver.hasSchemaObject(type = "view", name = it) }
            .mapTo(this) { "view:$it" }
    }

    if (missingObjects.isNotEmpty()) {
        throw SQLException("Missing required database schema objects: ${missingObjects.joinToString()}")
    }
}

private fun SqlDriver.hasSchemaObject(
    type: String,
    name: String,
): Boolean {
    val cursor = executeQuery(
        null,
        "SELECT count(*) FROM sqlite_master WHERE type = ? AND name = ?;",
        { QueryResult.Value(it.getLong(0)) },
        2,
        {
            bindString(0, type)
            bindString(1, name)
        },
    )
    return (cursor.value ?: 0L) > 0L
}

private val requiredTables = listOf(
    "blocked_word",
    "content_prefetch_queue",
    "deleted_feed_items",
    "feed_item_ids_for_checks",
    "feed_item_status",
    "feed_source",
    "feed_item",
    "feed_search",
    "feed_source_category",
    "feed_source_preferences",
    "read_status_pending_action",
    "sync_metadata",
)

private val requiredViews = listOf(
    "feed_source_unread_count",
    "category_with_unread",
)

private fun dropAllUserObjects(driver: SqlDriver) {
    val viewsCursor = driver.executeQuery(
        null,
        "SELECT group_concat(name, ',') FROM sqlite_master WHERE type='view';",
        { QueryResult.Value(it.getString(0)) },
        0,
        null,
    )
    viewsCursor.value?.split(',')?.filter { it.isNotBlank() }?.forEach { viewName ->
        driver.execute(null, "DROP VIEW IF EXISTS \"$viewName\";", 0, null)
    }

    val tablesCursor = driver.executeQuery(
        null,
        "SELECT group_concat(name, ',') FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';",
        { QueryResult.Value(it.getString(0)) },
        0,
        null,
    )
    driver.execute(null, "PRAGMA foreign_keys=OFF;", 0, null)
    tablesCursor.value?.split(',')?.filter { it.isNotBlank() }?.forEach { tableName ->
        driver.execute(null, "DROP TABLE IF EXISTS \"$tableName\";", 0, null)
    }
    driver.execute(null, "PRAGMA foreign_keys=ON;", 0, null)
}

private fun deleteDatabaseFiles(databasePath: File, logger: Logger) {
    val walFile = File("${databasePath.absolutePath}-wal")
    val shmFile = File("${databasePath.absolutePath}-shm")

    runCatching {
        if (databasePath.exists()) {
            databasePath.delete()
            logger.d("Deleted database file")
        }
    }
    runCatching {
        if (walFile.exists()) {
            walFile.delete()
            logger.d("Deleted WAL file")
        }
    }
    runCatching {
        if (shmFile.exists()) {
            shmFile.delete()
            logger.d("Deleted SHM file")
        }
    }
}
