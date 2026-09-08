package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB

actual fun createInMemoryDriver(): SqlDriver {
    Class.forName("org.sqlite.JDBC")
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    FeedFlowDB.Schema.create(driver)
    return driver
}

actual fun createInMemorySyncDriver(): SqlDriver {
    Class.forName("org.sqlite.JDBC")
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    FeedFlowFeedSyncDB.Schema.create(driver)
    return driver
}
