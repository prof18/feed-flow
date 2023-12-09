package com.prof18.feedflow.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB

fun createDatabaseDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = FeedFlowDB.Schema,
        name = DatabaseHelper.DATABASE_NAME,
    )
}
