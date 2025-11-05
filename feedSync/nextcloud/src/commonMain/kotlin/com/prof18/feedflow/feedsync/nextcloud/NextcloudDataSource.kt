package com.prof18.feedflow.feedsync.nextcloud

interface NextcloudDataSource {

    fun setup(credentials: NextcloudCredentials)

    fun isConfigured(): Boolean

    fun getCredentials(): NextcloudCredentials?

    suspend fun testConnection(): Boolean

    suspend fun performUpload(uploadParam: NextcloudUploadParam): NextcloudUploadResult

    suspend fun performDownload(downloadParam: NextcloudDownloadParam): NextcloudDownloadResult

    suspend fun revokeAccess()
}
