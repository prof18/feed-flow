package com.prof18.feedflow.feedsync.googledrive

import com.prof18.feedflow.core.model.GoogleDriveClientStatus

interface GoogleDriveDataSource {

    fun setup(clientId: String)

    fun startAuthorization(platformAuthHandler: () -> Unit)

    fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit)

    fun restoreAuth(stringCredentials: GoogleDriveStringCredentials): GoogleDriveClientStatus

    fun saveAuth(stringCredentials: GoogleDriveStringCredentials)

    suspend fun revokeAccess()

    fun isClientSet(): Boolean

    suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult

    suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult
}
