package com.prof18.feedflow.feedsync.database.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

class FeedSyncScope

internal expect fun getPlatformModule(appEnvironment: AppEnvironment): Module

internal val commonModule = module {
    single {
        SyncedDatabaseHelper(
            backgroundDispatcher = Dispatchers.IO,
        )
    }
}

fun getFeedSyncModule(appEnvironment: AppEnvironment) = commonModule + getPlatformModule(appEnvironment)

internal const val FEED_SYNC_SCOPE_NAME = "FeedSyncScope"
internal const val SYNC_DB_DRIVER = "SyncDBDriver"
