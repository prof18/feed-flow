package com.prof18.feedflow.feedsync.dropbox

import com.prof18.feedflow.core.model.DropboxClientStatus

interface DropboxDataSource {

    fun setup(apiKey: String)

    fun startAuthorization(platformAuthHandler: () -> Unit)

    fun handleOAuthResponse(platformOAuthResponseHandler: () -> Unit)

    fun restoreAuth(stringCredentials: DropboxStringCredentials): DropboxClientStatus

    fun saveAuth(stringCredentials: DropboxStringCredentials)

    /**
     * Can throw a [DropboxException]
     */
    suspend fun revokeAccess()

    fun isClientSet(): Boolean

    /**
     * If successful returns a [DropboxUploadResult] otherwise throws a [DropboxUploadException]
     */
    suspend fun performUpload(uploadParam: DropboxUploadParam): DropboxUploadResult

    /**
     * If successful returns a [DropboxDownloadResult] otherwise throws a [DropboxUploadException]
     */
    suspend fun performDownload(downloadParam: DropboxDownloadParam): DropboxDownloadResult
}
