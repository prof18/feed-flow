package com.prof18.feedflow.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.db.FeedFlowDB

fun createDatabaseDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = FeedFlowDB.Schema,
        context = context,
        name = DatabaseHelper.DATABASE_NAME,
    )
}
