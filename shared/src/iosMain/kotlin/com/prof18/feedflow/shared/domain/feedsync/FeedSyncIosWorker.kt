package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.SyncAccounts
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
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.domain.settings.SettingsRepository
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
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

internal class FeedSyncIosWorker(
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dropboxDataSource: DropboxDataSource,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val appEnvironment: AppEnvironment,
    private val dropboxSettings: DropboxSettings,
    private val settingsRepository: SettingsRepository,
    private val accountsRepository: AccountsRepository,
    private val iCloudSettings: ICloudSettings,
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
        try {
            feedSyncer.populateSyncDbIfEmpty()
            feedSyncer.updateFeedItemsToSyncDatabase()
            feedSyncer.closeDB()

            val databasePath = getDatabaseUrl()
            if (databasePath == null) {
                emitErrorMessage()
                return@withContext
            }
            accountSpecificUpload(databasePath)
            settingsRepository.setIsSyncUploadRequired(false)
            emitSuccessMessage()
        } catch (e: Exception) {
            logger.e("Upload to dropbox failed", e)
            emitErrorMessage()
        }
    }

    override suspend fun download(): SyncResult = withContext(dispatcherProvider.io) {
        return@withContext try {
            feedSyncer.closeDB()
            accountSpecificDownload()
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

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun accountSpecificUpload(databasePath: NSURL) =
        when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()
                val dropboxUploadParam = DropboxUploadParam(
                    path = "/${getDatabaseName()}.db",
                    url = databasePath,
                )
                dropboxDataSource.performUpload(dropboxUploadParam)
                dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to dropbox successfully" }
            }

            SyncAccounts.ICLOUD -> {
                val iCloudUrl = getICloudFolderURL()
                if (iCloudUrl != null) {
                    memScoped {
                        val errorPtr: ObjCObjectVar<NSError?> = alloc()

                        // Copy doesn't override the item, so we need to clear it before.
                        // An alternative would be checking the existence of the file before and copy or replace.
                        NSFileManager.defaultManager.removeItemAtURL(
                            iCloudUrl,
                            null,
                        )

                        NSFileManager.defaultManager.copyItemAtURL(
                            srcURL = databasePath,
                            toURL = iCloudUrl,
                            error = errorPtr.ptr,
                        )

                        if (errorPtr.value != null) {
                            logger.e { "Error uploading to iCloud: ${errorPtr.value}" }
                        }
                    }
                }
                iCloudSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to iCloud successfully" }
            }

            else -> {
                // Do nothing
            }
        }

    private suspend fun accountSpecificDownload(): SyncResult {
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                val dropboxDownloadParam = DropboxDownloadParam(
                    path = "/${getDatabaseName()}.db",
                    outputName = "${getDatabaseName()}.db",
                )

                restoreDropboxClient()
                val result = dropboxDataSource.performDownload(dropboxDownloadParam)
                val destinationUrl = result.destinationUrl
                if (destinationUrl == null) {
                    logger.e { "Error downloading database" }
                    return SyncResult.Error
                }
                replaceDatabase(destinationUrl.url)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                SyncResult.Success
            }

            SyncAccounts.ICLOUD -> {
                return iCloudDownload()
            }

            else -> {
                // Do nothing
                SyncResult.Success
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun iCloudDownload(): SyncResult {
        val iCloudUrl = getICloudFolderURL()
        val tempUrl = getTemporaryFileUrl()
        if (iCloudUrl == null || tempUrl == null) {
            logger.e { "Error downloading database" }
            return SyncResult.Error
        }
        NSFileManager.defaultManager.removeItemAtURL(
            tempUrl,
            null,
        )

        memScoped {
            val errorPtr: ObjCObjectVar<NSError?> = alloc()

            NSFileManager.defaultManager.copyItemAtURL(
                srcURL = iCloudUrl,
                toURL = tempUrl,
                error = errorPtr.ptr,
            )

            if (errorPtr.value != null) {
                logger.e { "Error downloading from iCloud: ${errorPtr.value}" }
                return SyncResult.Error
            }

            replaceDatabase(tempUrl)
            iCloudSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
            logger.d { "Download from iCloud successfully" }
            return SyncResult.Success
        }
    }

    private fun getICloudFolderURL(): NSURL? = NSFileManager.defaultManager
        .URLForUbiquityContainerIdentifier("iCloud.com.prof18.feedflow")
        ?.URLByAppendingPathComponent("Documents")
        ?.URLByAppendingPathComponent(getDatabaseName())

    private fun getTemporaryFileUrl(): NSURL? {
        val documentsDirectory: NSURL? = NSFileManager.defaultManager.URLsForDirectory(
            directory = NSDocumentDirectory,
            inDomains = NSUserDomainMask,
        ).firstOrNull() as? NSURL?
        val databaseUrl = documentsDirectory?.URLByAppendingPathComponent(getDatabaseName())

        return databaseUrl
    }
}
