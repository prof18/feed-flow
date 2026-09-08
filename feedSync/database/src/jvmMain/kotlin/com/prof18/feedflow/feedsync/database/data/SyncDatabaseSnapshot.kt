package com.prof18.feedflow.feedsync.database.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

fun prepareSyncDatabaseFile(file: File) {
    JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}").use { prepareSyncDatabaseSnapshot(it) }
}
