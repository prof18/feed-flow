package com.prof18.feedflow.feedsync.lan.di

import android.content.Context
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.lan.LanDiscoveryService
import com.prof18.feedflow.feedsync.lan.LanDiscoveryServiceAndroid
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.io.FileInputStream

actual fun platformLanModule(): Module = module {
    single<LanDiscoveryService> {
        LanDiscoveryServiceAndroid(
            context = get(),
            logger = get(),
        )
    }

    single {
        val context: Context = get()
        val appEnvironment: AppEnvironment = get()
        LanSyncServer(
            settings = get(),
            getDatabaseFileBytes = {
                val databaseName = if (appEnvironment.isDebug()) {
                    SYNC_DATABASE_NAME_DEBUG
                } else {
                    SYNC_DATABASE_NAME_PROD
                }
                val databasePath = context.getDatabasePath(databaseName).toString()
                val dbFile = File(databasePath)
                if (dbFile.exists()) {
                    try {
                        FileInputStream(dbFile).use { it.readBytes() }
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
        val context: Context = get()
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
                val databasePath = context.getDatabasePath(databaseName).toString()
                val dbFile = File(databasePath)
                try {
                    dbFile.writeBytes(bytes)
                    true
                } catch (e: Exception) {
                    false
                }
            },
        )
    }
}
