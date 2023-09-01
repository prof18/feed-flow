package com.prof18.feedflow

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import co.touchlab.kermit.Logger
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import java.io.File
import java.util.Properties

internal fun initDatabase(logger: Logger): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath()

    val databasePath = File(appPath, "/${DatabaseHelper.DB_FILE_NAME_WITH_EXTENSION}")

    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + databasePath.absolutePath, Properties())

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
        setVersion(driver, 1)
        logger.d("init: created tables, setVersion to 1")
    } else {
        val schemaVer = FeedFlowDB.Schema.version
        if (schemaVer > currentVer) {
            FeedFlowDB.Schema.migrate(driver, currentVer, schemaVer)
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
