package com.prof18.feedflow.shared.domain.feedsync

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.model.CloudBackupNotFoundException
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
import com.prof18.feedflow.feedsync.database.data.prepareSyncDatabaseFile
import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxSettings
import com.prof18.feedflow.feedsync.dropbox.DropboxStringCredentials
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadConflictException
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveNeedsReAuthException
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.icloud.ICloudSettings
import com.prof18.feedflow.shared.data.SettingsRepository
import com.prof18.feedflow.shared.utils.isTemporaryNetworkError
import com.prof18.feedflow.shared.utils.skipLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Clock

internal class FeedSyncJvmWorker(
    private val dropboxDataSource: DropboxDataSource,
    private val googleDriveDataSource: GoogleDriveDataSourceJvm,
    private val appEnvironment: AppEnvironment,
    private val logger: Logger,
    private val feedSyncer: FeedSyncer,
    private val pendingCloudChanges: PendingCloudChangesManager,
    private val feedSyncMessageQueue: FeedSyncMessageQueue,
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val dropboxSettings: DropboxSettings,
    private val googleDriveSettings: GoogleDriveSettings,
    private val accountsRepository: AccountsRepository,
    private val iCloudSettings: ICloudSettings,
    private val syncDirectory: File = File(AppDataPathBuilder.getAppDataPath(appEnvironment)),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val iCloudBridge: ICloudFileTransfer = ICloudNativeBridge(),
) : FeedSyncWorker {

    private val mutex = Mutex()
    private var downloadSession: String? = null
    private var dropboxRevision: String? = null

    private val appPath = syncDirectory.path
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
        mutex.withLock {
            var snapshot: File? = null
            try {
                val uploadSession = requireNotNull(pendingCloudChanges.sessionForEdit())
                retryDropboxConflicts {
                    snapshot?.delete()
                    snapshot = null
                    val uploadAccount = accountsRepository.getCurrentSyncAccount()
                    if (uploadAccount != SyncAccounts.ICLOUD) {
                        val result = downloadLocked(
                            expectedSession = uploadSession,
                        )
                        pendingCloudChanges.checkAccountSession(uploadSession)
                        when {
                            result is SyncResult.BackupNotFound -> feedSyncer.prepareInitialUpload()
                            result.isError() -> {
                                feedSyncMessageQueue.emitResult(result)
                                return@withLock
                            }
                        }
                        if (uploadAccount == SyncAccounts.DROPBOX && result !is SyncResult.BackupNotFound) {
                            requireNotNull(dropboxRevision) { "Dropbox download did not return a revision" }
                        }
                    } else {
                        feedSyncer.populateSyncDbIfEmpty()
                        feedSyncer.updateFeedItemsToSyncDatabase()
                    }
                    val pendingBatch = pendingCloudChanges.capturePendingChanges()
                    val uploadGeneration = settingsRepository.captureSyncUploadGeneration()
                    pendingCloudChanges.applyChangesToSyncDatabase(pendingBatch)
                    val uploaded = if (uploadAccount == SyncAccounts.ICLOUD) {
                        feedSyncer.withClosedDatabase {
                            pendingCloudChanges.checkAccountSession(uploadSession)
                            accountSpecificUpload(databaseFile, uploadAccount)
                        }
                    } else {
                        snapshot = File.createTempFile("cloud-upload-", ".db", syncDirectory)
                        feedSyncer.withClosedDatabase {
                            databaseFile.copyTo(requireNotNull(snapshot), overwrite = true)
                        }
                        pendingCloudChanges.checkAccountSession(uploadSession)
                        accountSpecificUpload(requireNotNull(snapshot), uploadAccount)
                    }

                    if (uploaded) {
                        pendingCloudChanges.markChangesAsUploaded(pendingBatch)
                        settingsRepository.acknowledgeSyncUpload(uploadGeneration)
                        emitSuccessMessage()
                    }
                }
            } catch (e: GoogleDriveNeedsReAuthException) {
                logger.d("Google Drive needs re-authorization", e)
                feedSyncMessageQueue.emitResult(SyncResult.GoogleDriveNeedReAuth())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!e.isTemporaryNetworkError()) {
                    logger.e("Upload to dropbox failed", e)
                }
                emitErrorMessage()
            } finally {
                snapshot?.delete()
            }
        }
    }

    private suspend fun accountSpecificUpload(uploadFile: File, account: SyncAccounts): Boolean =
        when (account) {
            SyncAccounts.DROPBOX -> {
                restoreDropboxClient()

                val dropboxUploadParam = DropboxUploadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    file = uploadFile,
                    expectedRevision = dropboxRevision,
                )

                val result = dropboxDataSource.performUpload(dropboxUploadParam)
                if (result.isConflict) throw DropboxUploadConflictException()
                dropboxSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to dropbox successfully" }
                true
            }

            SyncAccounts.ICLOUD -> {
                val result = iCloudBridge.uploadToICloud(appEnvironment.isDebug())
                when (UploadResult.fromCode(result)) {
                    UploadResult.SUCCESS -> {
                        iCloudSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                        logger.d { "Upload to iCloud successfully" }
                    }

                    UploadResult.ICLOUD_FOLDER_URL_NULL -> {
                        logger.d { "iCloud folder URL is null" }
                    }

                    UploadResult.UPLOAD_ERROR -> {
                        logger.d { "Error during iCloud upload" }
                    }

                    UploadResult.UNKNOWN_ERROR -> {
                        logger.d { "Unknown error during iCloud upload. Check the enum mapping" }
                    }
                }
                if (UploadResult.fromCode(result) == UploadResult.SUCCESS) {
                    true
                } else {
                    emitErrorMessage()
                    false
                }
            }

            SyncAccounts.GOOGLE_DRIVE -> {
                restoreGoogleDriveClient()
                if (!googleDriveDataSource.isClientSet()) {
                    return false
                }

                val googleDriveUploadParam = GoogleDriveUploadParam(
                    fileName = getDatabaseNameWithExtension(),
                    file = uploadFile,
                )

                googleDriveDataSource.performUpload(googleDriveUploadParam)
                googleDriveSettings.setLastUploadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Upload to Google Drive successfully" }
                true
            }

            SyncAccounts.LOCAL,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> true
        }

    override suspend fun download(isFirstSync: Boolean): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock { downloadLocked() }
    }

    private suspend fun downloadLocked(
        expectedSession: String? = null,
    ): SyncResult {
        var stagedFile: File? = null
        return try {
            dropboxRevision = null
            downloadSession = pendingCloudChanges.sessionForEdit()
            if (expectedSession != null) pendingCloudChanges.checkAccountSession(expectedSession)
            feedSyncer.resetDownloadedSnapshotState()
            stagedFile = File.createTempFile("cloud-download-", ".db", syncDirectory)
            accountSpecificDownload(stagedFile)
        } catch (_: CloudBackupNotFoundException) {
            SyncResult.BackupNotFound(syncDownloadErrorForAccount(accountsRepository.getCurrentSyncAccount()))
        } catch (e: GoogleDriveNeedsReAuthException) {
            logger.d("Google Drive needs re-authorization", e)
            SyncResult.GoogleDriveNeedReAuth()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val currentAccount = accountsRepository.getCurrentSyncAccount()
            val downloadError = syncDownloadErrorForAccount(currentAccount)
            logger.d(e) { "Download failed for account $currentAccount" }
            SyncResult.General(downloadError)
        } finally {
            stagedFile?.delete()
        }
    }

    private suspend fun accountSpecificDownload(stagedFile: File): SyncResult {
        return when (accountsRepository.getCurrentSyncAccount()) {
            SyncAccounts.DROPBOX -> {
                val dropboxDownloadParam = DropboxDownloadParam(
                    path = "/${getDatabaseNameWithExtension()}",
                    outputStream = FileOutputStream(stagedFile),
                )

                restoreDropboxClient()
                val result = dropboxDownloadParam.outputStream.use {
                    dropboxDataSource.performDownload(dropboxDownloadParam)
                }
                dropboxRevision = result.revision
                installDownloadedFile(stagedFile)
                dropboxSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                SyncResult.Success
            }
            SyncAccounts.ICLOUD -> {
                val result = iCloudBridge.downloadToFile(appEnvironment.isDebug(), stagedFile.absolutePath)
                when (DownloadResult.fromCode(result)) {
                    DownloadResult.SUCCESS -> {
                        installDownloadedFile(stagedFile)
                        iCloudSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                        logger.d { "Download from iCloud successfully" }
                        SyncResult.Success
                    }

                    DownloadResult.URL_NULL -> {
                        logger.d { "iCloud URL is null" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.TEMP_URL_NULL -> {
                        logger.d { "Temporary URL is null" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.DOWNLOAD_ERROR -> {
                        logger.d { "Error during iCloud download" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.DATABASE_REPLACE_ERROR -> {
                        logger.d { "Error during database replace" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }

                    DownloadResult.FILE_NOT_FOUND ->
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)

                    DownloadResult.UNKNOWN_ERROR -> {
                        logger.d { "Unknown error during iCloud download. Check the enum mapping" }
                        SyncResult.General(SyncDownloadError.ICloudDownloadFailed)
                    }
                }
            }
            SyncAccounts.GOOGLE_DRIVE -> {
                restoreGoogleDriveClient()
                if (!googleDriveDataSource.isClientSet()) {
                    return SyncResult.General(SyncDownloadError.GoogleDriveDownloadFailed)
                }

                val googleDriveDownloadParam = GoogleDriveDownloadParam(
                    fileName = getDatabaseNameWithExtension(),
                    outputStream = FileOutputStream(stagedFile),
                )

                googleDriveDownloadParam.outputStream.use {
                    googleDriveDataSource.performDownload(googleDriveDownloadParam)
                }
                installDownloadedFile(stagedFile)
                googleDriveSettings.setLastDownloadTimestamp(Clock.System.now().toEpochMilliseconds())
                logger.d { "Download from Google Drive successfully" }
                SyncResult.Success
            }

            SyncAccounts.LOCAL,
            SyncAccounts.FRESH_RSS,
            SyncAccounts.MINIFLUX,
            SyncAccounts.BAZQUX,
            SyncAccounts.FEEDBIN,
            -> {
                logger.d { "current sync account does not require cloud download" }
                SyncResult.Success
            }
        }
    }

    override suspend fun syncFeedSources(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                feedSyncer.syncFeedSourceCategory(downloadSession)
                feedSyncer.syncFeedSource()
                SyncResult.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("Sync feed sources failed", e)
                SyncResult.General(SyncFeedError.FeedSourcesSyncFailed)
            }
        }
    }

    override suspend fun syncFeedItems(): SyncResult = withContext(dispatcherProvider.io) {
        mutex.withLock {
            try {
                feedSyncer.syncFeedItem(downloadSession)
                SyncResult.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!e.skipLogging()) {
                    logger.e("Sync feed items failed", e)
                }
                SyncResult.General(SyncFeedError.FeedItemsSyncFailed)
            }
        }
    }

    private suspend fun installDownloadedFile(stagedFile: File) {
        prepareSyncDatabaseFile(stagedFile)
        feedSyncer.withClosedDatabase {
            pendingCloudChanges.withAccountSession(downloadSession) {
                Files.move(
                    stagedFile.toPath(),
                    databaseFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
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

    private suspend fun restoreGoogleDriveClient() {
        if (!googleDriveDataSource.isClientSet()) {
            googleDriveDataSource.restoreAuth().also { restored ->
                if (!restored) {
                    logger.d { "Google Drive client could not be restored" }
                    feedSyncMessageQueue.emitResult(SyncResult.GoogleDriveNeedReAuth())
                }
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
