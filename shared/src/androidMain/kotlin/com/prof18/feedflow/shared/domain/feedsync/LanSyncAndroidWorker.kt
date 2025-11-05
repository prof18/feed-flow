package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.time.Clock

internal class LanSyncAndroidWorker(
    private val context: Context,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dispatcherProvider: DispatcherProvider,
    private val settingsRepository: SettingsRepository,
    private val lanSyncRepository: LanSyncRepository,
    private val lanSyncServer: LanSyncServer,
) {

    suspend fun performUpload(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            lanSyncRepository.initialize()
            val deviceId = lanSyncRepository.settings.getDeviceId() ?: return@withContext SyncResult.General(
                SyncUploadError.DatabaseFileGeneration,
            )
            val deviceName = lanSyncRepository.settings.getDeviceName() ?: "FeedFlow Device"
            val port = lanSyncRepository.settings.getServerPort()

            if (!lanSyncServer.isRunning()) {
                lanSyncServer.start()
            }

            lanSyncRepository.discoveryService.advertiseService(deviceId, deviceName, port)
            lanSyncRepository.updateLocalTimestamp()

            settingsRepository.setIsSyncUploadRequired(false)
            logger.d { "LAN sync upload completed - server running and advertising" }
            emitSuccessMessage()
            return@withContext SyncResult.Success
        } catch (e: Exception) {
            logger.e("LAN sync upload failed", e)
            emitErrorMessage(SyncUploadError.DropboxUploadFailed)
            return@withContext SyncResult.General(SyncUploadError.DropboxUploadFailed)
        }
    }

    suspend fun download(): SyncResult = withContext(dispatcherProvider.io) {
        return@withContext try {
            feedSyncer.closeDB()

            val devices = mutableListOf<com.prof18.feedflow.feedsync.lan.LanDevice>()
            lanSyncRepository.getDiscoveredDevices().collect { discoveredDevices ->
                devices.clear()
                devices.addAll(discoveredDevices)
                return@collect
            }

            if (devices.isEmpty()) {
                logger.d { "No LAN devices discovered" }
                return@withContext SyncResult.Success
            }

            val latestDevice = devices.maxByOrNull {
                lanSyncRepository.syncClient.fetchMetadata(it)?.timestamp ?: 0L
            }

            if (latestDevice == null) {
                logger.d { "No device with valid metadata found" }
                return@withContext SyncResult.Success
            }

            val result = lanSyncRepository.syncWithDevice(latestDevice)
            when (result) {
                is LanSyncRepository.SyncResult.Success -> {
                    logger.d { "LAN sync download successful" }
                    SyncResult.Success
                }
                is LanSyncRepository.SyncResult.UpToDate -> {
                    logger.d { "Local data is up-to-date" }
                    SyncResult.Success
                }
                is LanSyncRepository.SyncResult.Failure -> {
                    logger.e { "LAN sync download failed: ${result.message}" }
                    SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
                }
            }
        } catch (e: Exception) {
            logger.e("Download from LAN failed", e)
            SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
        }
    }

    suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedSourceCategory()
            feedSyncer.syncFeedSource()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed sources failed", e)
            SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
        }
    }

    suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedItem()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed items failed", e)
            SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
        }
    }

    fun getDatabaseFile(): File? {
        val databasePath = context.getDatabasePath(getDatabaseName()).toString()
        val dbFile = File(databasePath)
        return if (dbFile.exists()) dbFile else null
    }

    fun getDatabaseFileBytes(): ByteArray? {
        val dbFile = getDatabaseFile() ?: return null
        return try {
            FileInputStream(dbFile).use { it.readBytes() }
        } catch (e: Exception) {
            logger.e(e) { "Failed to read database file" }
            null
        }
    }

    fun saveDatabaseFile(bytes: ByteArray): Boolean {
        val databasePath = context.getDatabasePath(getDatabaseName()).toString()
        val dbFile = File(databasePath)
        return try {
            dbFile.writeBytes(bytes)
            true
        } catch (e: Exception) {
            logger.e(e) { "Failed to save database file" }
            false
        }
    }

    private fun getDatabaseName(): String {
        return if (appEnvironment.isDebug()) {
            SYNC_DATABASE_NAME_DEBUG
        } else {
            SYNC_DATABASE_NAME_PROD
        }
    }

    private suspend fun emitErrorMessage(error: SyncUploadError) =
        feedSyncMessageQueue.emitResult(SyncResult.General(error))

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)
}
