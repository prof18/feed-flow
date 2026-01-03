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
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
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
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveNeedsReAuthException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.shared.data.SettingsRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Clock

internal class FeedSyncAndroidWorker(
    private val context: Context,
    private val dropboxDataSource: DropboxDataSource,
    private val googleDriveDataSource: GoogleDriveDataSourceAndroid,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dispatcherProvider: DispatcherProvider,
    private val dropboxSettings: DropboxSettings,
    private val googleDriveSettings: GoogleDriveSettings,
    private val settingsRepository: SettingsRepository,
    private val accountsRepository: AccountsRepository,
) : FeedSyncWorker {

    private val mutex = Mutex()

    override suspend fun uploadImmediate() {
        logger.d { "Start Immediate upload" }
        performUpload()
    }

    override fun upload() {
        logger.d { "Enqueue upload" }
        val uploadWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<SyncWorkManager>()
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
        mutex.withLock {
            try {
                feedSyncer.populateSyncDbIfEmpty()
                feedSyncer.updateFeedItemsToSyncDatabase()
                feedSyncer.closeDB()

                val databaseFile = generateDatabaseFile()
                    ?: return@withContext SyncResult.General(SyncUploadError.DatabaseFileGeneration)

                accountSpecificUpload(databaseFile)
                emitSuccessMessage()
                settingsRepository.setIsSyncUploadRequired(false)
                return@withContext SyncResult.Success
            } catch (e: GoogleDriveNeedsReAuthException) {
                logger.e("Google Drive needs re-authorization", e)
                SyncResult.GoogleDriveNeedReAuth()
            } catch (e: Exception) {
                logger.e("Upload failed", e)
                emitErrorMessage(SyncUploadError.DropboxUploadFailed)
                return@withLock SyncResult.General(SyncUploadError.DropboxUploadFailed)
            }
        }
    }

    private suspend fun accountSpecificUpload(databaseFile: File) {
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()
                val dropboxUploadParam = DropboxUploadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    file = databaseFile,
                )
                dropboxDataSource.performUpload(dropboxUploadParam)
                dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to Dropbox successfully" }
            }

            SyncAccounts.GOOGLE_DRIVE -> {
                val googleDriveUploadParam = GoogleDriveUploadParam(
                    fileName = getDatabaseNameWithExtension(),
                    file = databaseFile,
                )
                googleDriveDataSource.performUpload(googleDriveUploadParam)
                googleDriveSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to Google Drive successfully" }
            }

            SyncAccounts.LOCAL,
            SyncAccounts.ICLOUD,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> {
                // Do nothing
            }
        }
    }

    override suspend fun download(isFirstSync: Boolean): SyncResult = withContext(dispatcherProvider.io) {
        return@withContext mutex.withLock {
            try {
                feedSyncer.closeDB()
                accountSpecificDownload()
            } catch (e: GoogleDriveNeedsReAuthException) {
                logger.e("Google Drive needs re-authorization", e)
                SyncResult.GoogleDriveNeedReAuth()
            } catch (e: Exception) {
                logger.e("Download failed", e)
                SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
            }
        }
    }

    private suspend fun accountSpecificDownload(): SyncResult {
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()
                val databaseLocalPath = databasePath()
                val dropboxDownloadParam = DropboxDownloadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    outputStream = FileOutputStream(databaseLocalPath),
                )
                dropboxDataSource.performDownload(dropboxDownloadParam)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Download from Dropbox successfully" }
                SyncResult.Success
            }

            SyncAccounts.GOOGLE_DRIVE -> {
                val databaseLocalPath = databasePath()
                val googleDriveDownloadParam = GoogleDriveDownloadParam(
                    fileName = getDatabaseNameWithExtension(),
                    outputStream = FileOutputStream(databaseLocalPath),
                )
                googleDriveDataSource.performDownload(googleDriveDownloadParam)
                googleDriveSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Download from Google Drive successfully" }
                SyncResult.Success
            }

            SyncAccounts.LOCAL,
            SyncAccounts.ICLOUD,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> {
                SyncResult.Success
            }
        }
    }

    override suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                feedSyncer.syncFeedSourceCategory()
                feedSyncer.syncFeedSource()
                SyncResult.Success
            } catch (e: Exception) {
                logger.e("Sync feed sources failed", e)
                SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
            }
        }
    }

    override suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                feedSyncer.syncFeedItem()
                SyncResult.Success
            } catch (e: Exception) {
                logger.e("Sync feed items failed", e)
                SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
            }
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
