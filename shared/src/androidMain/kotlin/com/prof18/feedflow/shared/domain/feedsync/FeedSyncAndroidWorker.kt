package com.prof18.feedflow.shared.domain.feedsync

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.SyncResult
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
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

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
                .build()

        WorkManager.getInstance(context).enqueue(uploadWorkRequest)
    }

    internal suspend fun performUpload(): SyncResult = withContext(dispatcherProvider.io) {
        restoreDropboxClient()

        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            val databaseFile = generateDatabaseFile() ?: return@withContext SyncResult.Error
            val dropboxUploadParam = DropboxUploadParam(
                path = "/${getDatabaseNameWithExtension()}",
                file = databaseFile,
            )
            dropboxDataSource.performUpload(dropboxUploadParam)
            dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
            logger.d { "Upload to dropbox successfully" }
            emitSuccessMessage()
            settingsRepository.setIsSyncUploadRequired(false)
            return@withContext SyncResult.Success
        } catch (e: Exception) {
            logger.e("Upload to dropbox failed", e)
            emitErrorMessage()
            return@withContext SyncResult.Error
        }
    }

    override suspend fun download(): SyncResult = withContext(dispatcherProvider.io) {
        val databaseLocalPath = databasePath()
        val dropboxDownloadParam = DropboxDownloadParam(
            path = "/${getDatabaseNameWithExtension()}",
            outputStream = FileOutputStream(databaseLocalPath),
        )

        restoreDropboxClient()

        return@withContext try {
            feedSyncer.closeDB()
            dropboxDataSource.performDownload(dropboxDownloadParam)
            dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Download from dropbox failed", e)
            SyncResult.Error
        }
    }

    override suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedSourceCategory()
            feedSyncer.syncFeedSource()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed sources failed", e)
            SyncResult.Error
        }
    }

    override suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            feedSyncer.syncFeedItem()
            SyncResult.Success
        } catch (e: Exception) {
            logger.e("Sync feed items failed", e)
            SyncResult.Error
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
                emitErrorMessage()
            }
        }
    }

    private fun getDatabaseNameWithExtension(): String =
        "${getDatabaseName()}.db"

    private suspend fun emitErrorMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Error)

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)

    private companion object {
        const val BUFFER_SIZE = 1024
    }
}
