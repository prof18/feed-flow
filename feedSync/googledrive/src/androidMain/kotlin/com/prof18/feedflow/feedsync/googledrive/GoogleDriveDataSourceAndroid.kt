package com.prof18.feedflow.feedsync.googledrive

interface GoogleDriveDataSourceAndroid {
    suspend fun isAuthorized(): Boolean

    fun revokeAccess()

    suspend fun validateAuthorization(): AuthorizationValidationResult

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult

    /**
     * Can throw also [GoogleDriveNeedsReAuthException]
     */
    suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult
}
