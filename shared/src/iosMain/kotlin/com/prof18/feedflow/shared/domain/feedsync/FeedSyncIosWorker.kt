package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
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
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.URLByAppendingPathComponent

internal class FeedSyncIosWorker(
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dropboxDataSource: DropboxDataSource,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val appEnvironment: AppEnvironment,
    private val dropboxSettings: DropboxSettings,
    private val settingsHelper: SettingsHelper,
) : FeedSyncWorker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun upload() {
        scope.launch {
            logger.d { "Enqueue upload" }
            performUpload()
        }
    }

    override suspend fun uploadImmediate() {
        logger.d { "Start Immediate upload" }
        performUpload()
    }

    private suspend fun performUpload() = withContext(dispatcherProvider.io) {
        restoreDropboxClient()

        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            val databasePath = getDatabaseUrl()
            if (databasePath == null) {
                emitErrorMessage()
                return@withContext
            }
            val dropboxUploadParam = DropboxUploadParam(
                path = "/${getDatabaseName()}.db",
                url = databasePath,
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
        val dropboxDownloadParam = DropboxDownloadParam(
            path = "/${getDatabaseName()}.db",
            outputName = "${getDatabaseName()}.db",
        )

        restoreDropboxClient()

        return@withContext try {
            feedSyncer.closeDB()
            val result = dropboxDataSource.performDownload(dropboxDownloadParam)
            val destinationUrl = result.destinationUrl
            if (destinationUrl == null) {
                logger.e { "Error downloading database" }
                return@withContext SyncResult.Error
            }
            replaceDatabase(destinationUrl.url)
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
            // String credentials are not used on iOS
            dropboxDataSource.restoreAuth(DropboxStringCredentials(""))

            if (!dropboxDataSource.isClientSet()) {
                logger.d { "Dropbox client is null" }
                emitErrorMessage()
            }
        }
    }

    private suspend fun emitErrorMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Error)

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)

    private fun getDatabaseName(): String {
        return if (appEnvironment.isDebug()) {
            SYNC_DATABASE_NAME_DEBUG
        } else {
            SYNC_DATABASE_NAME_PROD
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun getDatabaseUrl(): NSURL? {
        memScoped {
            val errorPtr: ObjCObjectVar<NSError?> = alloc()
            val documentsDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = errorPtr.ptr,
            )
            val databaseUrl = documentsDirectory?.URLByAppendingPathComponent("databases/${getDatabaseName()}")

            if (errorPtr.value != null) {
                logger.e { "Error getting database URL: ${errorPtr.value}" }
                return null
            }
            return databaseUrl
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun replaceDatabase(url: NSURL): Boolean {
        val dbUrl = getDatabaseUrl()
        if (dbUrl != null) {
            // Replace the database
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                NSFileManager.defaultManager.replaceItemAtURL(
                    originalItemURL = dbUrl,
                    withItemAtURL = url,
                    backupItemName = "${getDatabaseName()}.old",
                    options = NSFileManagerItemReplacementUsingNewMetadataOnly,
                    error = errorPtr.ptr,
                    resultingItemURL = null,
                )

                if (errorPtr.value != null) {
                    logger.e { "Error replacing database: ${errorPtr.value}" }
                    return false
                }

                return true
            }
        }
        return false
    }
}
