package com.prof18.feedflow.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.db.FeedFlowDB

fun createDatabaseDriver(appEnvironment: AppEnvironment): SqlDriver {
    return NativeSqliteDriver(
        schema = FeedFlowDB.Schema,
        onConfiguration = { conf ->
            conf.copy(
                extendedConfig = conf.extendedConfig.copy(
                    basePath = getAppGroupDatabasePath(),
                ),
            )
        },
        name = if (appEnvironment.isDebug()) {
            DatabaseHelper.APP_DATABASE_NAME_DEBUG
        } else {
            DatabaseHelper.APP_DATABASE_NAME_PROD
        },
    )
}
