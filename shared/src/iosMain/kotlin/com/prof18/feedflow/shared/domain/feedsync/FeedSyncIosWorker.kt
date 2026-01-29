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
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceIos
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.icloud.ICloudDataSource
import com.prof18.feedflow.feedsync.icloud.ICloudDownloadResult
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.feedsync.icloud.ICloudUploadResult
import com.prof18.feedflow.shared.data.SettingsRepository
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileManagerItemReplacementUsingNewMetadataOnly
import platform.Foundation.NSURL
import kotlin.coroutines.resume
import kotlin.time.Clock

internal class FeedSyncIosWorker(
    private val dispatcherProvider: DispatcherProvider,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val dropboxDataSource: DropboxDataSource,
    private val googleDriveDataSource: GoogleDriveDataSourceIos,
    private val iCloudDataSource: ICloudDataSource,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val appEnvironment: AppEnvironment,
    private val dropboxSettings: DropboxSettings,
    private val googleDriveSettings: GoogleDriveSettings,
    private val settingsRepository: SettingsRepository,
    private val accountsRepository: AccountsRepository,
    private val iCloudSettings: ICloudSettings,
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
                    emitErrorMessage()
                    return@withLock
                }
                accountSpecificUpload(databasePath)
                settingsRepository.setIsSyncUploadRequired(false)
                emitSuccessMessage()
            } catch (e: SQLiteException) {
                logger.e(e) { "SQLiteException during upload" }
                try {
                    feedSyncer.closeDB()
                    logger.e { "Sync database closed after error" }
                    getDatabaseUrl()?.let { path ->
                        NSFileManager.defaultManager.removeItemAtURL(path, null)
                        feedSyncer.populateSyncDbIfEmpty()
                        logger.e { "Sync database recreated after error" }
                    }
                } catch (_: Exception) {
                    // best effort
                    logger.e { "Database recreation after error failed" }
                }
            } catch (e: Exception) {
                logger.e("Upload failed", e)
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
                logger.w { "Start syncing feed items" }
                feedSyncer.syncFeedItem()
                logger.w { "Syncing feed items finished" }
                SyncResult.Success
            } catch (e: Exception) {
                logger.e("Sync feed items failed", e)
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

    private suspend fun restoreGoogleDriveClient() {
        if (!googleDriveDataSource.isServiceSet()) {
            val restored = suspendCancellableCoroutine { continuation ->
                googleDriveDataSource.restorePreviousSignIn { success ->
                    continuation.resume(success)
                }
            }
            if (!restored) {
                logger.d { "Google Drive service could not be restored" }
                feedSyncMessageQueue.emitResult(SyncResult.GoogleDriveNeedReAuth())
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
                iCloudUpload(databasePath)
            }

            SyncAccounts.GOOGLE_DRIVE -> {
                restoreGoogleDriveClient()
                googleDriveUpload(databasePath)
            }

            SyncAccounts.LOCAL,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> {
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

            SyncAccounts.GOOGLE_DRIVE -> {
                restoreGoogleDriveClient()
                return googleDriveDownload()
            }

            SyncAccounts.LOCAL,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> {
                // Do nothing
                SyncResult.Success
            }
        }
    }

    private suspend fun iCloudDownload(isFirstSync: Boolean): SyncResult {
        return when (val result = iCloudDataSource.performDownload(getDatabaseName())) {
            is ICloudDownloadResult.Success -> {
                replaceDatabase(result.destinationUrl)
                iCloudSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.w { "Download from iCloud successfully" }
                SyncResult.Success
            }

            is ICloudDownloadResult.Error -> {
                val errorMessage = when (result) {
                    is ICloudDownloadResult.Error.ICloudUrlNotAvailable -> "iCloud URL is not available"
                    is ICloudDownloadResult.Error.TemporaryUrlNotAvailable -> "Temporary URL is not available"
                    is ICloudDownloadResult.Error.FileNotFound -> "File not found in iCloud"
                    is ICloudDownloadResult.Error.CopyOperationFailed -> "Copy operation failed"
                    is ICloudDownloadResult.Error.FileAlreadyExists -> "File already exists"
                    is ICloudDownloadResult.Error.DownloadFailed -> result.errorMessage
                }
                if (!isFirstSync) {
                    logger.e { "Error downloading from iCloud: $errorMessage" }
                }
                when (result) {
                    is ICloudDownloadResult.Error.ICloudUrlNotAvailable ->
                        SyncResult.ICloudNotAvailable(SyncICloudError.URLNotAvailable)
                    is ICloudDownloadResult.Error.TemporaryUrlNotAvailable ->
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    is ICloudDownloadResult.Error.FileNotFound ->
                        SyncResult.General(SyncICloudError.FileNotFound)
                    is ICloudDownloadResult.Error.CopyOperationFailed ->
                        SyncResult.General(SyncICloudError.CopyOperationFailed)
                    is ICloudDownloadResult.Error.FileAlreadyExists ->
                        SyncResult.General(SyncICloudError.FileAlreadyExists)
                    is ICloudDownloadResult.Error.DownloadFailed ->
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                }
            }
        }
    }

    private suspend fun iCloudUpload(databasePath: NSURL) {
        when (val result = iCloudDataSource.performUpload(databasePath, getDatabaseName())) {
            is ICloudUploadResult.Success -> {
                iCloudSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.w { "Upload to iCloud successfully" }
            }

            is ICloudUploadResult.Error -> {
                val errorMessage = when (result) {
                    is ICloudUploadResult.Error.ICloudUrlNotAvailable -> "iCloud URL is not available"
                    is ICloudUploadResult.Error.UploadFailed -> result.errorMessage
                }
                logger.e { "Error uploading to iCloud: $errorMessage" }
            }
        }
    }

    private suspend fun googleDriveUpload(databasePath: NSURL) {
        val uploadParam = GoogleDriveUploadParam(
            fileName = "${getDatabaseName()}.db",
            url = databasePath,
        )
        googleDriveDataSource.performUpload(uploadParam)
        googleDriveSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
        logger.d { "Upload to Google Drive successfully" }
    }

    private suspend fun googleDriveDownload(): SyncResult {
        val downloadParam = GoogleDriveDownloadParam(
            fileName = "${getDatabaseName()}.db",
            outputName = "${getDatabaseName()}.db",
        )
        val result = googleDriveDataSource.performDownload(downloadParam)
        val destinationUrl = result.destinationUrl
        return if (destinationUrl == null) {
            logger.e { "Google Drive download: destination URL is null" }
            SyncResult.General(SyncDownloadError.GoogleDriveDownloadFailed)
        } else {
            replaceDatabase(destinationUrl.url)
            googleDriveSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
            logger.d { "Download from Google Drive successfully" }
            SyncResult.Success
        }
    }
}
