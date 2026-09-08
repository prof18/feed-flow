package com.prof18.feedflow.shared.test.cloudsync

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File
import java.nio.file.Files
import java.util.Properties

internal actual fun createCloudDevice(
    provider: CloudProvider,
    store: CloudStore,
    id: String,
    root: String?,
): CloudDevice {
    val directory = root?.let(::File) ?: Files.createTempDirectory("feedflow-android-cloud-$id-").toFile()
    return createAndroidCloudDevice(
        provider = provider,
        store = store,
        id = id,
        root = directory.absolutePath,
        driverFactory = ::jdbcDriver,
        useFixtureDatabaseDirectory = true,
        seedAccount = root == null,
    )
}

private fun jdbcDriver(file: File, isSyncDatabase: Boolean): SqlDriver {
    Class.forName("org.sqlite.JDBC")
    file.parentFile?.mkdirs()
    val existed = file.exists()
    return JdbcSqliteDriver(
        "jdbc:sqlite:${file.absolutePath}",
        Properties().apply {
            setProperty("journal_mode", "WAL")
            setProperty("synchronous", "NORMAL")
            setProperty("busy_timeout", "30000")
        },
    ).also { driver ->
        if (!existed) {
            if (isSyncDatabase) {
                FeedFlowFeedSyncDB.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = ${FeedFlowFeedSyncDB.Schema.version}", 0)
            } else {
                FeedFlowDB.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = ${FeedFlowDB.Schema.version}", 0)
            }
        }
    }
}
