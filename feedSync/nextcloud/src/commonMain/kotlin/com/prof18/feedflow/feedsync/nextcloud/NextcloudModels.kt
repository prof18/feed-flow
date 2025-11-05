package com.prof18.feedflow.feedsync.nextcloud

data class NextcloudUploadResult(
    val path: String,
    val lastModified: Long,
    val etag: String?,
    val sizeInBytes: Long,
)

data class NextcloudDownloadResult(
    val path: String,
    val lastModified: Long,
    val etag: String?,
    val sizeInBytes: Long,
)

data class NextcloudUploadParam(
    val remotePath: String,
    val localFilePath: String,
)

data class NextcloudDownloadParam(
    val remotePath: String,
    val destinationPath: String,
)

data class NextcloudCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
)
