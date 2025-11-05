package com.prof18.feedflow.feedsync.nextcloud

import co.touchlab.kermit.Logger

internal class NextcloudDataSourceImpl(
    private val logger: Logger,
    private val nextcloudClient: NextcloudClient,
    private val nextcloudSettings: NextcloudSettings,
    private val fileOperations: NextcloudFileOperations,
) : NextcloudDataSource {

    override fun setup(credentials: NextcloudCredentials) {
        nextcloudSettings.setServerUrl(credentials.serverUrl)
        nextcloudSettings.setUsername(credentials.username)
        nextcloudSettings.setPassword(credentials.password)
    }

    override fun isConfigured(): Boolean {
        return nextcloudSettings.hasCredentials()
    }

    override fun getCredentials(): NextcloudCredentials? {
        val serverUrl = nextcloudSettings.getServerUrl()
        val username = nextcloudSettings.getUsername()
        val password = nextcloudSettings.getPassword()

        return if (serverUrl != null && username != null && password != null) {
            NextcloudCredentials(serverUrl, username, password)
        } else {
            null
        }
    }

    override suspend fun testConnection(): Boolean {
        if (!isConfigured()) {
            return false
        }
        return try {
            nextcloudClient.testConnection()
        } catch (e: Exception) {
            logger.e(e) { "Nextcloud connection test failed" }
            false
        }
    }

    override suspend fun performUpload(uploadParam: NextcloudUploadParam): NextcloudUploadResult {
        if (!isConfigured()) {
            throw NextcloudException.AuthenticationException("Nextcloud not configured")
        }

        val fileContent = fileOperations.readFile(uploadParam.localFilePath)
        val result = nextcloudClient.upload(uploadParam.remotePath, fileContent)

        nextcloudSettings.setLastUploadTimestamp(result.lastModified)
        return result
    }

    override suspend fun performDownload(downloadParam: NextcloudDownloadParam): NextcloudDownloadResult {
        if (!isConfigured()) {
            throw NextcloudException.AuthenticationException("Nextcloud not configured")
        }

        val fileContent = nextcloudClient.download(downloadParam.remotePath)
        fileOperations.writeFile(downloadParam.destinationPath, fileContent)

        val lastModified = System.currentTimeMillis()
        nextcloudSettings.setLastDownloadTimestamp(lastModified)

        return NextcloudDownloadResult(
            path = downloadParam.remotePath,
            lastModified = lastModified,
            etag = null,
            sizeInBytes = fileContent.size.toLong(),
        )
    }

    override suspend fun revokeAccess() {
        nextcloudSettings.clearCredentials()
        nextcloudClient.closeClient()
    }
}
