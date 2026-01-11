package com.prof18.feedflow.shared.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB

actual fun createInMemoryDriver(): SqlDriver {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    FeedFlowDB.Schema.create(driver)
    return driver
}

