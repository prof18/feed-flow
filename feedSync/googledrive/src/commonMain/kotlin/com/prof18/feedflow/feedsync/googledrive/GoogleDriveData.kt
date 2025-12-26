package com.prof18.feedflow.feedsync.googledrive

// TODO: verify if both are necessary
data object GoogleDriveUploadResult

data class GoogleDriveDownloadResult(
    val destinationUrl: DatabaseDestinationUrl? = null,
)
