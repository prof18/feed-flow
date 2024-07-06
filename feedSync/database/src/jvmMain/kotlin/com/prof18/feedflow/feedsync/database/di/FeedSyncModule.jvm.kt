package com.prof18.feedflow.feedsync.database.di

import app.cash.sqldelight.db.SqlDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.createDatabaseDriver
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    scope(named(FEED_SYNC_SCOPE_NAME)) {
        scoped<SqlDriver>(named(SYNC_DB_DRIVER)) {
            createDatabaseDriver(
                appEnvironment = appEnvironment,
                logger = get(parameters = { parametersOf("initDatabase") }),
            )
        }
    }
}
