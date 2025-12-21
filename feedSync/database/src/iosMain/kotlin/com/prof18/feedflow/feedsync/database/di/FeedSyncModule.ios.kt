package com.prof18.feedflow.feedsync.database.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.JournalMode
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal actual fun getPlatformModule(appEnvironment: AppEnvironment): Module = module {
    scope(named(FEED_SYNC_SCOPE_NAME)) {
        scoped<SqlDriver>(named(SYNC_DB_DRIVER)) {
            val databaseName = if (appEnvironment.isDebug()) {
                SyncedDatabaseHelper.SYNC_DATABASE_NAME_DEBUG
            } else {
                SyncedDatabaseHelper.SYNC_DATABASE_NAME_PROD
            }

            NativeSqliteDriver(
                schema = FeedFlowFeedSyncDB.Schema,
                onConfiguration = { conf ->
                    conf.copy(
                        extendedConfig = conf.extendedConfig.copy(
                            basePath = getAppGroupDatabasePath(),
                            busyTimeout = 30000,
                        ),
                        journalMode = JournalMode.WAL,
                    )
                },
                name = databaseName,
            )
        }
    }
}
