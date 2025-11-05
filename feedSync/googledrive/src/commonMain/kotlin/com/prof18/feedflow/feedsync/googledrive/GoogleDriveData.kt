package com.prof18.feedflow.feedsync.googledrive

data class GoogleDriveUploadResult(
    val id: String,
    val editDateMillis: Long,
    val sizeInByte: Long,
)

data class GoogleDriveDownloadResult(
    val id: String,
    val sizeInByte: Long,
    val destinationUrl: DatabaseDestinationUrl? = null,
)
