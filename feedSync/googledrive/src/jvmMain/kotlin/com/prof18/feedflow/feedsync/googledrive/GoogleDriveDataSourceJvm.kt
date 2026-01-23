package com.prof18.feedflow.feedsync.googledrive

interface GoogleDriveDataSourceJvm {
    suspend fun startAuthFlow(): Boolean

    fun restoreAuth(): Boolean

    suspend fun revokeAccess()

    fun isClientSet(): Boolean

    suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult

    suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult
}
