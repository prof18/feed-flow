package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.ErrorCode
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.lan.LanSyncRepository
import com.prof18.feedflow.feedsync.lan.LanSyncServer
import com.prof18.feedflow.feedsync.lan.LanSyncSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Clock

internal class FeedSyncAndroidWorker(
    private val context: Context,
    private val dropboxDataSource: DropboxDataSource,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dispatcherProvider: DispatcherProvider,
    private val dropboxSettings: DropboxSettings,
    private val settingsRepository: SettingsRepository,
    private val accountsRepository: AccountsRepository,
    private val lanSyncRepository: LanSyncRepository,
    private val lanSyncServer: LanSyncServer,
    private val lanSyncSettings: LanSyncSettings,
) : FeedSyncWorker {

    override suspend fun uploadImmediate() {
        logger.d { "Start Immediate upload" }
        performUpload()
    }

    override fun upload() {
        logger.d { "Enqueue upload" }
        val uploadWorkRequest: WorkRequest =
            OneTimeWorkRequestBuilder<SyncWorkManager>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
    }

    internal suspend fun performUpload(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            accountSpecificUpload()

            emitSuccessMessage()
            settingsRepository.setIsSyncUploadRequired(false)
            return@withContext SyncResult.Success
        } catch (e: Exception) {
            logger.e("Upload failed", e)
            emitErrorMessage(SyncUploadError.DropboxUploadFailed)
            return@withContext SyncResult.General(SyncUploadError.DropboxUploadFailed)
        }
    }

    private suspend fun accountSpecificUpload() =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()
                val databaseFile = generateDatabaseFile()
                    ?: throw Exception("Database file generation failed")
                val dropboxUploadParam = DropboxUploadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    file = databaseFile,
                )
                dropboxDataSource.performUpload(dropboxUploadParam)
                dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to dropbox successfully" }
            }

            SyncAccounts.LAN -> {
                lanSyncRepository.initialize()
                val deviceId = lanSyncSettings.getDeviceId()
                    ?: throw Exception("Device ID not set")
                val deviceName = lanSyncSettings.getDeviceName() ?: "FeedFlow Device"
                val port = lanSyncSettings.getServerPort()

                if (!lanSyncServer.isRunning()) {
                    lanSyncServer.start()
                }

                lanSyncRepository.discoveryService.advertiseService(deviceId, deviceName, port)
                lanSyncRepository.updateLocalTimestamp()
                logger.d { "LAN sync upload completed - server running and advertising" }
            }

            else -> {
                // Do nothing
            }
        }

    override suspend fun download(isFirstSync: Boolean): SyncResult = withContext(dispatcherProvider.io) {
        return@withContext try {
            feedSyncer.closeDB()
            accountSpecificDownload()
        } catch (e: Exception) {
            logger.e("Download failed", e)
            SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
        }
    }

    private suspend fun accountSpecificDownload(): SyncResult {
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                val databaseLocalPath = databasePath()
                val dropboxDownloadParam = DropboxDownloadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    outputStream = FileOutputStream(databaseLocalPath),
                )
                restoreDropboxClient()
                dropboxDataSource.performDownload(dropboxDownloadParam)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                SyncResult.Success
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

            SyncAccounts.LOCAL, SyncAccounts.ICLOUD, SyncAccounts.FRESH_RSS -> {
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

    private fun generateDatabaseFile(): File? {
        val inFileName: String = databasePath()

        val dbFile = File(inFileName)
        val fis = FileInputStream(dbFile)
        val openFileOutput = context.openFileOutput(
            getDatabaseNameWithExtension(),
            Context.MODE_PRIVATE,
        ) ?: return null
        openFileOutput.use { output ->
            // Transfer bytes from the input file to the output file
            val buffer = ByteArray(BUFFER_SIZE)
            var length: Int
            while (fis.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }

            // Close the streams
            output.flush()
            fis.close()
            Logger.d { "Database file generated successfully" }

            return context.getFileStreamPath(getDatabaseNameWithExtension())
        }
    }

    private fun databasePath(): String =
        context.getDatabasePath(getDatabaseName()).toString()

    private fun getDatabaseName(): String {
        return if (appEnvironment.isDebug()) {
            SYNC_DATABASE_NAME_DEBUG
        } else {
            SYNC_DATABASE_NAME_PROD
        }
    }

    private suspend fun restoreDropboxClient() {
        if (!dropboxDataSource.isClientSet()) {
            val stringCredentials = dropboxSettings.getDropboxData()
            if (!stringCredentials.isNullOrEmpty()) {
                dropboxDataSource.restoreAuth(DropboxStringCredentials(stringCredentials))
            }

            if (!dropboxDataSource.isClientSet()) {
                logger.e { "Dropbox client is null" }
                emitErrorMessage(SyncUploadError.DropboxClientRestoreError)
            }
        }
    }

    private fun getDatabaseNameWithExtension(): String =
        "${getDatabaseName()}.db"

    private suspend fun emitErrorMessage(errorCode: ErrorCode) =
        feedSyncMessageQueue.emitResult(SyncResult.General(errorCode))

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)

    private companion object {
        const val BUFFER_SIZE = 1024
    }
}
