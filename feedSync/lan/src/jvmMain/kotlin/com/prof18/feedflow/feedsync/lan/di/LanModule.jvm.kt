package com.prof18.feedflow.feedsync.lan.di

import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.lan.LanDiscoveryService
import com.prof18.feedflow.feedsync.lan.LanDiscoveryServiceJvm
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual fun platformLanModule(): Module = module {
    single<LanDiscoveryService> {
        LanDiscoveryServiceJvm(
            logger = get(),
        )
    }

    single {
        val appEnvironment: AppEnvironment = get()
        LanSyncServer(
            settings = get(),
            getDatabaseFileBytes = {
                val databaseName = if (appEnvironment.isDebug()) {
                    SYNC_DATABASE_NAME_DEBUG
                } else {
                    SYNC_DATABASE_NAME_PROD
                }
                val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)
                val databaseFile = File(appPath, "/$databaseName.db")
                if (databaseFile.exists()) {
                    try {
                        databaseFile.readBytes()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            },
            logger = get(),
        )
    }

    single {
        val appEnvironment: AppEnvironment = get()
        LanSyncRepository(
            settings = get(),
            discoveryService = get(),
            syncServer = get(),
            syncClient = get(),
            logger = get(),
            saveDatabaseFile = { bytes ->
                val databaseName = if (appEnvironment.isDebug()) {
                    SYNC_DATABASE_NAME_DEBUG
                } else {
                    SYNC_DATABASE_NAME_PROD
                }
                val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)
                val databaseFile = File(appPath, "/$databaseName.db")
                try {
                    databaseFile.writeBytes(bytes)
                    true
                } catch (e: Exception) {
                    false
                }
            },
        )
    }
}
