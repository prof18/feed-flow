package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.feedsync.dropbox.DropboxDataSource
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxDownloadResult
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadParam
import com.prof18.feedflow.feedsync.dropbox.DropboxUploadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDataSourceJvm
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveDownloadResult
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveSettings
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadParam
import com.prof18.feedflow.feedsync.googledrive.GoogleDriveUploadResult
import com.prof18.feedflow.shared.domain.feedsync.ICloudFileTransfer
import com.prof18.feedflow.shared.test.DropboxDataSourceFake
import java.io.File

private const val ACCOUNT = "fixture-account"

internal class JvmDropboxTransport(
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
        downloadParam.outputStream.write(bytes)
        return DropboxDownloadResult(downloadParam.path, bytes.size.toLong(), null)
    }
}

internal class JvmDriveTransport(
    private val store: CloudStore,
    private val device: String,
    private val settings: GoogleDriveSettings,
) : GoogleDriveDataSourceJvm {
    override suspend fun startAuthFlow(): Boolean = true
    override fun restoreAuth(): Boolean = true
    override suspend fun revokeAccess() = Unit
    override fun isClientSet(): Boolean = true

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult {
        store.beforeUploadRead()
        val id = store.upload(
            CloudProvider.GOOGLE_DRIVE,
            ACCOUNT,
            uploadParam.fileName,
            uploadParam.file.readBytes(),
            device,
        )
        settings.setBackupFileId(id)
        return GoogleDriveUploadResult
    }

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult {
        downloadParam.outputStream.write(store.download(CloudProvider.GOOGLE_DRIVE, ACCOUNT, downloadParam.fileName))
        settings.setBackupFileId(store.snapshot().single().fileId)
        return GoogleDriveDownloadResult()
    }
}

internal class JvmICloudTransport(
    private val store: CloudStore,
    private val device: String,
    private val database: File,
) : ICloudFileTransfer {
    override fun uploadToICloud(isDebug: Boolean): Int {
        if (store.uploadFailure != null) return 2
        store.upload(CloudProvider.ICLOUD, ACCOUNT, database.nameWithoutExtension, database.readBytes(), device)
        return 0
    }

    override fun downloadToFile(isDebug: Boolean, destinationPath: String): Int {
        if (store.downloadFailure != null) return 3
        if (store.fileCount(CloudProvider.ICLOUD) == 0) return 5
        File(destinationPath).writeBytes(store.download(CloudProvider.ICLOUD, ACCOUNT, database.nameWithoutExtension))
        return 0
    }
}
