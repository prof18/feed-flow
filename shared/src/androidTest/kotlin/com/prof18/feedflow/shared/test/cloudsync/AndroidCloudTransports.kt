package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadResult
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadResult
import com.prof18.feedflow.feedsync.googledrive.AuthorizationValidationResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceAndroid
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadResult
import com.prof18.feedflow.shared.test.DropboxDataSourceFake

internal class AndroidDropboxTransport(
    private val store: CloudStore,
    private val device: String,
) : DropboxDataSource by DropboxDataSourceFake() {
    override suspend fun performUpload(uploadParam: DropboxUploadParam): DropboxUploadResult {
        store.beforeUploadRead()
        val bytes = uploadParam.file.readBytes()
        val id = store.upload(CloudProvider.DROPBOX, ACCOUNT, uploadParam.path.substringAfterLast('/'), bytes, device)
        return DropboxUploadResult(id, 1000, bytes.size.toLong(), null)
    }

    override suspend fun performDownload(downloadParam: DropboxDownloadParam): DropboxDownloadResult {
        val bytes = store.download(CloudProvider.DROPBOX, ACCOUNT, downloadParam.path.substringAfterLast('/'))
        downloadParam.outputStream.use { it.write(bytes) }
        return DropboxDownloadResult(downloadParam.path, bytes.size.toLong(), null)
    }
}

internal class AndroidDriveTransport(
    private val store: CloudStore,
    private val device: String,
    private val settings: GoogleDriveSettings,
) : GoogleDriveDataSourceAndroid {
    override suspend fun isAuthorized(): Boolean = true

    override fun revokeAccess() = Unit

    override suspend fun validateAuthorization(): AuthorizationValidationResult =
        AuthorizationValidationResult.Valid

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult {
        store.beforeUploadRead()
        val bytes = uploadParam.file.readBytes()
        val id = store.upload(CloudProvider.GOOGLE_DRIVE, ACCOUNT, uploadParam.fileName, bytes, device)
        settings.setBackupFileId(id)
        return GoogleDriveUploadResult
    }

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult {
        downloadParam.outputStream.use {
            it.write(store.download(CloudProvider.GOOGLE_DRIVE, ACCOUNT, downloadParam.fileName))
        }
        settings.setBackupFileId(store.snapshot().single().fileId)
        return GoogleDriveDownloadResult()
    }
}

private const val ACCOUNT = "fixture-account"
