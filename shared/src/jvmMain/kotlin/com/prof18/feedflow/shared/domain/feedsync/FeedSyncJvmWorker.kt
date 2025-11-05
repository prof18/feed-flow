package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import com.prof18.feedflow.feedsync.lan.LanSyncSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

internal class FeedSyncJvmWorker(
    private val dropboxDataSource: DropboxDataSource,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val dropboxSettings: DropboxSettings,
    private val accountsRepository: AccountsRepository,
    private val iCloudSettings: ICloudSettings,
    private val lanSyncRepository: LanSyncRepository,
    private val lanSyncServer: LanSyncServer,
    private val lanSyncSettings: LanSyncSettings,
) : FeedSyncWorker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appPath = AppDataPathBuilder.getAppDataPath(appEnvironment)
    private val databaseName = if (appEnvironment.isDebug()) {
        SYNC_DATABASE_NAME_DEBUG
    } else {
        SYNC_DATABASE_NAME_PROD
    }
    private val databaseFile = File(appPath, "/$databaseName.db")

    override suspend fun uploadImmediate() {
        logger.d { "Start Immediate upload" }
        performUpload()
    }

    override fun upload() {
        scope.launch {
            logger.d { "Enqueue upload" }
            performUpload()
        }
    }

    private suspend fun performUpload() = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            accountSpecificUpload()

            settingsRepository.setIsSyncUploadRequired(false)
            emitSuccessMessage()
        } catch (e: Exception) {
            logger.e("Upload to dropbox failed", e)
            emitErrorMessage()
        }
    }

    private suspend fun accountSpecificUpload() =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()

                val dropboxUploadParam = DropboxUploadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    file = databaseFile,
                )

                dropboxDataSource.performUpload(dropboxUploadParam)
                dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to dropbox successfully" }
            }

            SyncAccounts.ICLOUD -> {
                val result = ICloudNativeBridge().uploadToICloud(appEnvironment.isDebug())
                when (UploadResult.fromCode(result)) {
                    UploadResult.SUCCESS -> {
                        iCloudSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                        logger.d { "Upload to iCloud successfully" }
                    }

                    UploadResult.ICLOUD_FOLDER_URL_NULL -> {
                        logger.e { "iCloud folder URL is null" }
                    }

                    UploadResult.UPLOAD_ERROR -> {
                        logger.e { "Error during iCloud upload" }
                    }

                    UploadResult.UNKNOWN_ERROR -> {
                        logger.e { "Unknown error during iCloud upload. Check the enum mapping" }
                    }
                }
            }

            SyncAccounts.LAN -> {
                lanSyncRepository.initialize()
                val deviceId = lanSyncSettings.getDeviceId()
                if (deviceId != null) {
                    val deviceName = lanSyncSettings.getDeviceName() ?: "FeedFlow Device"
                    val port = lanSyncSettings.getServerPort()

                    if (!lanSyncServer.isRunning()) {
                        lanSyncServer.start()
                    }

                    lanSyncRepository.discoveryService.advertiseService(deviceId, deviceName, port)
                    lanSyncRepository.updateLocalTimestamp()
                    logger.d { "LAN sync upload completed - server running and advertising" }
                }
            }

            SyncAccounts.LOCAL, SyncAccounts.FRESH_RSS -> {
                // Do nothing
            }
        }

    override suspend fun download(isFirstSync: Boolean): SyncResult = withContext(dispatcherProvider.io) {
        try {
            databaseFile.delete()
        } catch (_: Exception) {
            // do nothing
        }

        return@withContext try {
            feedSyncer.closeDB()
            accountSpecificDownload()
        } catch (e: Exception) {
            logger.e("Download from dropbox failed", e)
            SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
        }
    }

    private suspend fun accountSpecificDownload(): SyncResult {
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                val dropboxDownloadParam = DropboxDownloadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    outputStream = FileOutputStream(databaseFile),
                )

                restoreDropboxClient()
                dropboxDataSource.performDownload(dropboxDownloadParam)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                SyncResult.Success
            }
            SyncAccounts.ICLOUD -> {
                val result = ICloudNativeBridge().iCloudDownload(appEnvironment.isDebug())
                when (DownloadResult.fromCode(result)) {
                    DownloadResult.SUCCESS -> {
                        iCloudSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                        logger.d { "Download from iCloud successfully" }
                        SyncResult.Success
                    }

                    DownloadResult.URL_NULL -> {
                        logger.e { "iCloud URL is null" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.TEMP_URL_NULL -> {
                        logger.e { "Temporary URL is null" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.DOWNLOAD_ERROR -> {
                        logger.e { "Error during iCloud download" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.DATABASE_REPLACE_ERROR -> {
                        logger.e { "Error during database replace" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.UNKNOWN_ERROR -> {
                        logger.e { "Unknown error during iCloud download. Check the enum mapping" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }
                }
            }
            SyncAccounts.LAN -> {
                val devices = mutableListOf<com.prof18.feedflow.feedsync.lan.LanDevice>()
                lanSyncRepository.getDiscoveredDevices().collect { discoveredDevices ->
                    devices.clear()
                    devices.addAll(discoveredDevices)
                    return@collect
                }

                if (devices.isEmpty()) {
                    logger.d { "No LAN devices discovered" }
                    return SyncResult.Success
                }

                val latestDevice = devices.maxByOrNull {
                    lanSyncRepository.syncClient.fetchMetadata(it)?.timestamp ?: 0L
                }

                if (latestDevice == null) {
                    logger.d { "No device with valid metadata found" }
                    return SyncResult.Success
                }

                when (val result = lanSyncRepository.syncWithDevice(latestDevice)) {
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
            }

            SyncAccounts.LOCAL, SyncAccounts.FRESH_RSS -> {
                // Do nothing
                logger.d { "current sync account local" }
                SyncResult.Success
            }
        }
    }

    override suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedSourceCategory()
            feedSyncer.syncFeedSource()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed sources failed", e)
            SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
        }
    }

    override suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedItem()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed items failed", e)
            SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
        }
    }

    private suspend fun restoreDropboxClient() {
        if (!dropboxDataSource.isClientSet()) {
            val stringCredentials = dropboxSettings.getDropboxData()
            if (stringCredentials != null) {
                dropboxDataSource.restoreAuth(DropboxStringCredentials(stringCredentials))
            }

            if (!dropboxDataSource.isClientSet()) {
                logger.d { "Dropbox client is null" }
                emitErrorMessage()
            }
        }
    }

    private fun getDatabaseName(): String {
        return if (appEnvironment.isDebug()) {
            SYNC_DATABASE_NAME_DEBUG
        } else {
            SYNC_DATABASE_NAME_PROD
        }
    }

    private fun getDatabaseNameWithExtension(): String =
        "${getDatabaseName()}.db"

    private suspend fun emitErrorMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.General(SyncUploadError.DropboxUploadFailed))

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)
}
