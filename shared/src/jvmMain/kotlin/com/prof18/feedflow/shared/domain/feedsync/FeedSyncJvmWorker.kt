package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.AppDataPathBuilder
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.shared.data.SettingsHelper
import com.prof18.feedflow.shared.domain.model.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.io.File
import java.io.FileOutputStream

internal class FeedSyncJvmWorker(
    private val dropboxDataSource: DropboxDataSource,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val settingsHelper: SettingsHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val dropboxSettings: DropboxSettings,
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
        restoreDropboxClient()

        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.closeDB()

            val dropboxUploadParam = DropboxUploadParam(
                path = "/${getDatabaseNameWithExtension()}",
                file = databaseFile,
            )

            dropboxDataSource.performUpload(dropboxUploadParam)
            dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
            logger.d { "Upload to dropbox successfully" }
            settingsHelper.setIsSyncUploadRequired(false)
            emitSuccessMessage()
        } catch (e: Exception) {
            logger.e("Upload to dropbox failed", e)
            emitErrorMessage()
        }
    }

    override suspend fun download(): SyncResult = withContext(dispatcherProvider.io) {
        try {
            databaseFile.delete()
        } catch (_: Exception) {
            // do nothing
        }
        val dropboxDownloadParam = DropboxDownloadParam(
            path = "/${getDatabaseNameWithExtension()}",
            outputStream = FileOutputStream(databaseFile),
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
        feedSyncMessageQueue.emitResult(SyncResult.Error)

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)
}
