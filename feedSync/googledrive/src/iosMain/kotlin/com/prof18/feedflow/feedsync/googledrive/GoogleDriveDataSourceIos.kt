package com.prof18.feedflow.feedsync.googledrive

import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider
import com.prof18.feedflow.core.utils.getAppGroupDatabasePath
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.coroutines.resume

class GoogleDriveDataSourceIos(
    private val platformClient: GoogleDrivePlatformClientIos,
    private val googleDriveSettings: GoogleDriveSettings,
    private val logger: Logger,
    private val dispatcherProvider: DispatcherProvider,
) {

    fun authenticate(onResult: (Boolean) -> Unit) {
        platformClient.authenticate(onResult)
    }

    fun restorePreviousSignIn(onResult: (Boolean) -> Unit) {
        platformClient.restorePreviousSignIn(onResult)
    }

    fun isAuthorized(): Boolean = platformClient.isAuthorized()

    fun isServiceSet(): Boolean = platformClient.isServiceSet()

    fun revokeAccess() {
        platformClient.signOut()
        googleDriveSettings.clearAll()
    }

    suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        withContext(dispatcherProvider.io) {
            suspendCancellableCoroutine { continuation ->
                val url = uploadParam.url
                val data = NSData.create(contentsOfURL = url) ?: run {
                    logger.e { "Failed to read file data from URL" }
                    continuation.resume(GoogleDriveUploadResult)
                    return@suspendCancellableCoroutine
                }

                val cachedFileId = googleDriveSettings.getBackupFileId()

                platformClient.uploadFile(
                    data = data,
                    fileName = uploadParam.fileName,
                    existingFileId = cachedFileId,
                ) { fileId, error ->
                    if (error != null) {
                        logger.e { "Upload failed: ${error.message}" }
                    } else if (fileId != null) {
                        googleDriveSettings.setBackupFileId(fileId)
                    }
                    continuation.resume(GoogleDriveUploadResult)
                }
            }
        }

    suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        withContext(dispatcherProvider.io) {
            suspendCancellableCoroutine { continuation ->
                val cachedFileId = googleDriveSettings.getBackupFileId()

                platformClient.downloadFile(
                    fileName = downloadParam.fileName,
                    existingFileId = cachedFileId,
                ) { data, error ->
                    if (error != null) {
                        logger.e { "Download failed: ${error.message}" }
                        continuation.resume(GoogleDriveDownloadResult(destinationUrl = null))
                        return@downloadFile
                    }

                    if (data == null) {
                        logger.e { "Download returned null data" }
                        continuation.resume(GoogleDriveDownloadResult(destinationUrl = null))
                        return@downloadFile
                    }

                    // Write data to file
                    val destUrl = NSURL.fileURLWithPath(getAppGroupDatabasePath())
                        .URLByAppendingPathComponent(downloadParam.outputName)

                    if (destUrl != null) {
                        data.writeToURL(destUrl, atomically = true)
                        continuation.resume(GoogleDriveDownloadResult(destinationUrl = DatabaseDestinationUrl(destUrl)))
                    } else {
                        continuation.resume(GoogleDriveDownloadResult(destinationUrl = null))
                    }
                }
            }
        }
}
