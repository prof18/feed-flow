package com.prof18.feedflow.feedsync.database.di

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    scope<FeedSyncScope> {
        scoped {
            AndroidSqliteDriver(
                schema = FeedFlowFeedSyncDB.Schema,
                context = get(),
                name = if (appEnvironment.isDebug()) {
                    SyncedDatabaseHelper.DATABASE_NAME_DEBUG
                } else {
                    SyncedDatabaseHelper.DATABASE_NAME_PROD
                },
            )
        }
    }
}
