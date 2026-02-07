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
        if (databasePath.exists()) {
            databasePath.delete()
        }
        driver = createDriver()
        return initDatabase(driver, logger)
    }
}

private fun initDatabase(driver: SqlDriver, logger: Logger): SqlDriver {
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
        FeedFlowDB.Schema.create(driver)
        setVersion(driver, FeedFlowDB.Schema.version)
        logger.d("init: created tables, setVersion to ${FeedFlowDB.Schema.version}")
    } else {
        val schemaVer = FeedFlowDB.Schema.version
        if (schemaVer > currentVer) {
            FeedFlowDB.Schema.migrate(driver, oldVersion = currentVer, newVersion = schemaVer)
            setVersion(driver, schemaVer)
            logger.d("init: migrated from $currentVer to $schemaVer")
        } else {
            logger.d("init with existing database")
        }
    }
    return driver
}

fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}
