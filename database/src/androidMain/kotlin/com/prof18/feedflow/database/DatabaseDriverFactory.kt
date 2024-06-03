package com.prof18.feedflow.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.db.FeedFlowDB

fun createDatabaseDriver(context: Context, appEnvironment: AppEnvironment): SqlDriver {
    return AndroidSqliteDriver(
        schema = FeedFlowDB.Schema,
        context = context,
        name = if (appEnvironment.isDebug()) {
            DatabaseHelper.DATABASE_NAME_DEBUG
        } else {
            DatabaseHelper.DATABASE_NAME_PROD
        },
    )
}
