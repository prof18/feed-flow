package com.prof18.feedflow

import co.touchlab.kermit.Logger
import com.prof18.feedflow.data.DatabaseHelper
import com.prof18.feedflow.db.FeedFlowDB
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.io.File
import java.util.Properties

internal fun initDatabase(): SqlDriver {
    val appPath = AppDataPathBuilder.getAppDataPath()

    val databasePath = File(appPath, "/${DatabaseHelper.DB_FILE_NAME_WITH_EXTENSION}")

    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY + databasePath.absolutePath, Properties())

    val sqlCursor = driver.executeQuery(null, "PRAGMA user_version;", 0, null)
    val currentVer: Int = sqlCursor.use { sqlCursor.getLong(0)?.toInt() ?: 0 }

    if (currentVer == 0) {
        FeedFlowDB.Schema.create(driver)
        setVersion(driver, 1)
        Logger.d("init: created tables, setVersion to 1")
    } else {
        val schemaVer: Int = FeedFlowDB.Schema.version
        if (schemaVer > currentVer) {
            FeedFlowDB.Schema.migrate(driver, currentVer, schemaVer)
            setVersion(driver, schemaVer)
            Logger.d("init: migrated from $currentVer to $schemaVer")
        } else {
            Logger.d("init")
        }
    }
    return driver
}

fun setVersion(driver: SqlDriver, version: Int) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}
