package com.prof18.feedflow.feedsync.lan.di

import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.lan.LanDiscoveryService
import com.prof18.feedflow.feedsync.lan.LanDiscoveryServiceIos
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL

actual fun platformLanModule(): Module = module {
    single<LanDiscoveryService> {
        LanDiscoveryServiceIos(
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
                getDatabaseBytesIos(databaseName)
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
                saveDatabaseBytesIos(databaseName, bytes)
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun getDatabaseBytesIos(databaseName: String): ByteArray? {
    val databaseUrl = NSURL.fileURLWithPath(getAppGroupDatabasePath())
        .URLByAppendingPathComponent(databaseName)
        ?: return null

    val data = NSData.dataWithContentsOfURL(databaseUrl) ?: return null
    return ByteArray(data.length.toInt()).apply {
        data.getBytes(this.refTo(0), data.length)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun saveDatabaseBytesIos(databaseName: String, bytes: ByteArray): Boolean {
    val databaseUrl = NSURL.fileURLWithPath(getAppGroupDatabasePath())
        .URLByAppendingPathComponent(databaseName)
        ?: return false

    val data = NSData.create(bytes = bytes)
    return data.writeToURL(databaseUrl, atomically = true)
}
