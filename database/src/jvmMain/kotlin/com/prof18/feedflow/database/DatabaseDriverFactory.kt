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
import java.util.Properties

fun createDatabaseDriver(
    appEnvironment: AppEnvironment,
    logger: Logger,
): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)

    val databasePath = File(appPath, "/${DatabaseHelper.DB_FILE_NAME_WITH_EXTENSION}")

    fun createDriver(): JdbcSqliteDriver {
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
    } catch (_: java.sql.SQLException) {
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
            throw java.sql.SQLException("inconsistent database state: tables exist but user_version is 0")
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
