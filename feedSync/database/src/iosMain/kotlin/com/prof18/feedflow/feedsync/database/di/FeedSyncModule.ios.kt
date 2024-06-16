package com.prof18.feedflow.feedsync.database.di

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    scope<FeedSyncScope> {
        scoped {
            NativeSqliteDriver(
                schema = FeedFlowFeedSyncDB.Schema,
                name = if (appEnvironment.isDebug()) {
                    SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
                } else {
                    SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD
                },
            )
        }
    }
}
