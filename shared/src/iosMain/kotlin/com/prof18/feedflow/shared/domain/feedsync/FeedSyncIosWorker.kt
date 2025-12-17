package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import co.touchlab.sqliter.interop.SQLiteException
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncICloudError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.AppEnvironment
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_DEBUG
import com.prof18.feedflow.feedsync.database.data.SyncedDatabaseHelper.Companion.SYNC_DATABASE_NAME_PROD
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.presentation.getICloudBaseFolderURL
import com.prof18.feedflow.shared.utils.Telemetry
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.time.Clock

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
    private val telemetry: Telemetry,
) : FeedSyncWorker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

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

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun performUpload() = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                logger.w { "Starting upload" }
                feedSyncer.populateSyncDbIfEmpty()
                feedSyncer.updateFeedItemsToSyncDatabase()
                feedSyncer.closeDB()
                logger.w { "Sync database populated and closed" }

                val databasePath = getDatabaseUrl()
                if (databasePath == null) {
                    logger.e { "Database URL is null, cannot perform upload" }
                    telemetry.trackError(
                        id = "FeedSyncIosWorker.performUpload",
                        message = "Database URL is null, cannot perform upload",
                    )
                    emitErrorMessage()
                    return@withLock
                }
                accountSpecificUpload(databasePath)
                settingsRepository.setIsSyncUploadRequired(false)
                emitSuccessMessage()
            } catch (e: SQLiteException) {
                logger.e(e) { "SQLiteException during upload" }
                telemetry.trackError(
                    id = "FeedSyncIosWorker.performUpload",
                    message = "SQLiteException during upload: ${e.message}",
                )
                try {
                    feedSyncer.closeDB()
                    logger.e { "Sync database closed after error" }
                    getDatabaseUrl()?.let { path ->
                        NSFileManager.defaultManager.removeItemAtURL(path, null)
                        feedSyncer.populateSyncDbIfEmpty()
                        logger.e { "Sync database recreated after error" }
                        telemetry.signal("FeedSyncIosWorker.performUpload.recreateDatabase")
                    }
                } catch (_: Exception) {
                    // best effort
                    logger.e { "Database recreation after error failed" }
                    telemetry.trackError(
                        id = "FeedSyncIosWorker.performUpload",
                        message = "Database recreation after error failed",
                    )
                }
            } catch (e: Exception) {
                logger.e("Upload failed", e)
                telemetry.trackError(
                    id = "FeedSyncIosWorker.performUpload",
                    message = "Upload failed: ${e.message}",
                )
                if (e.message?.contains("FeedFlow.DropboxErrors") == true) {
                    feedSyncMessageQueue.emitResult(SyncResult.General(SyncUploadError.DropboxAPIError))
                } else {
                    emitErrorMessage()
                }
            }
        }
    }

    override suspend fun download(isFirstSync: Boolean): SyncResult = withContext(dispatcherProvider.io) {
        return@withContext mutex.withLock {
            try {
                feedSyncer.closeDB()
                accountSpecificDownload(isFirstSync)
            } catch (e: Exception) {
                if (!isFirstSync) {
                    logger.e("Download failed", e)
                    if (accountsRepository.getCurrentSyncAccount() == SyncAccounts.ICLOUD) {
                        telemetry.trackError(
                            id = "FeedSyncIosWorker.download",
                            message = "Download failed: ${e.message}",
                        )
                    }
                }
                SyncResult.General(SyncDownloadError.DropboxDownloadFailed)
            }
        }
    }

    override suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                logger.w { "Start syncing feed sources" }
                feedSyncer.syncFeedSourceCategory()
                feedSyncer.syncFeedSource()
                logger.w { "Syncing feed sources finished" }
                if (accountsRepository.getCurrentSyncAccount() == SyncAccounts.ICLOUD) {
                    telemetry.signal("FeedSyncIosWorker.syncFeedSources.iCloud")
                }
                SyncResult.Success
            } catch (e: Exception) {
                logger.e("Sync feed sources failed", e)
                telemetry.trackError(
                    id = "FeedSyncIosWorker.syncFeedSources",
                    message = "Sync feed sources failed: ${e.message}",
                )
                SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
            }
        }
    }

    override suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                logger.w { "Start syncing feed items" }
                feedSyncer.syncFeedItem()
                logger.w { "Syncing feed items finished" }
                if (accountsRepository.getCurrentSyncAccount() == SyncAccounts.ICLOUD) {
                    telemetry.signal("FeedSyncIosWorker.syncFeedItems.iCloud")
                }
                SyncResult.Success
            } catch (e: Exception) {
                logger.e("Sync feed items failed", e)
                telemetry.trackError(
                    id = "FeedSyncIosWorker.syncFeedItems",
                    message = "Sync feed items failed: ${e.message}",
                )
                SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
            }
        }
    }

    private suspend fun restoreDropboxClient() {
        if (!dropboxDataSource.isClientSet()) {
            // String credentials are not used on iOS
            dropboxDataSource.restoreAuth(DropboxStringCredentials(""))

            if (!dropboxDataSource.isClientSet()) {
                logger.d { "Dropbox client is null" }
                feedSyncMessageQueue.emitResult(SyncResult.General(SyncUploadError.DropboxClientRestoreError))
            }
        }
    }

    private suspend fun emitErrorMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.General(SyncUploadError.DropboxUploadFailed))

    private suspend fun emitSuccessMessage() =
        feedSyncMessageQueue.emitResult(SyncResult.Success)

    private fun getDatabaseName(): String {
        return if (appEnvironment.isDebug()) {
            SYNC_DATABASE_NAME_DEBUG
        } else {
            SYNC_DATABASE_NAME_PROD
        }
    }

    private fun getDatabaseUrl(): NSURL? =
        NSURL.fileURLWithPath(getAppGroupDatabasePath())
            .URLByAppendingPathComponent(getDatabaseName())

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
                    telemetry.trackError(
                        id = "FeedSyncIosWorker.replaceDatabase",
                        message = "Error replacing database: ${errorPtr.value}",
                    )
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
                logger.w { "Upload to dropbox successfully" }
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
                            telemetry.trackError(
                                id = "FeedSyncIosWorker.accountSpecificUpload",
                                message = "Error uploading to iCloud: ${errorPtr.value}",
                            )
                        }
                    }
                    iCloudSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                    logger.w { "Upload to iCloud successfully" }
                    telemetry.signal("FeedSyncIosWorker.accountSpecificUpload.iCloud")
                } else {
                    logger.e { "Error uploading to iCloud: iCloud URL is null" }
                    telemetry.trackError(
                        id = "FeedSyncIosWorker.accountSpecificUpload",
                        message = "Error uploading to iCloud: iCloud URL is null",
                    )
                }
            }

            else -> {
                // Do nothing
            }
        }

    private suspend fun accountSpecificDownload(isFirstSync: Boolean): SyncResult {
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
                    return SyncResult.General(SyncICloudError.DestinationUrlNull)
                }
                replaceDatabase(destinationUrl.url)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.w { "Download from Dropbox successfully" }
                SyncResult.Success
            }

            SyncAccounts.ICLOUD -> {
                return iCloudDownload(isFirstSync)
            }

            else -> {
                // Do nothing
                SyncResult.Success
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun iCloudDownload(isFirstSync: Boolean): SyncResult {
        val iCloudUrl = getICloudFolderURL()
        val tempUrl = getTemporaryFileUrl()
        if (iCloudUrl == null || tempUrl == null) {
            val iCloudUrlNull = iCloudUrl == null
            val tempUrlNull = tempUrl == null
            val message =
                """
                    Error downloading from iCloud: iCloud URL is null: $iCloudUrlNull, temporary URL is null: $tempUrlNull
                """.trimIndent()
            logger.e { message }
            telemetry.trackError(
                id = "FeedSyncIosWorker.iCloudDownload",
                message = message,
            )
            return if (iCloudUrl == null) {
                SyncResult.ICloudNotAvailable(SyncICloudError.URLNotAvailable)
            } else {
                SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
            }
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
                if (!isFirstSync) {
                    logger.e { "Error downloading from iCloud: ${errorPtr.value}" }
                    telemetry.trackError(
                        id = "FeedSyncIosWorker.iCloudDownload",
                        message = "Error downloading from iCloud: ${errorPtr.value}",
                    )
                }
                val error = errorPtr.value.toString()
                return when {
                    error.contains("Code=260") || error.contains("Code=4") -> SyncResult.General(
                        SyncICloudError.FileNotFound,
                    )
                    error.contains("Code=512") -> SyncResult.General(SyncICloudError.CopyOperationFailed)
                    error.contains("Code=516") -> SyncResult.General(SyncICloudError.FileAlreadyExists)
                    else -> SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                }
            }

            replaceDatabase(tempUrl)
            iCloudSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
            logger.w { "Download from iCloud successfully" }
            telemetry.signal("FeedSyncIosWorker.iCloudDownload")
            return SyncResult.Success
        }
    }

    private suspend fun getICloudFolderURL(): NSURL? = getICloudBaseFolderURL()
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
