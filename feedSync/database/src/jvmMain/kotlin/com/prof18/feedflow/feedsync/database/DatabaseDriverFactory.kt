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

fun setVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version;", 0, null)
}
