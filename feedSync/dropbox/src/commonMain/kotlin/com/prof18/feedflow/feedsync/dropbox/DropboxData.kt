package com.prof18.feedflow.feedsync.dropbox

data class DropboxUploadResult(
    val id: String,
    val editDateMillis: Long,
    val sizeInByte: Long,
    val contentHash: String?,
    val revision: String? = null,
    val isConflict: Boolean = false,
)

data class DropboxDownloadResult(
    val id: String,
    val sizeInByte: Long,
    val contentHash: String?,
    val destinationUrl: DatabaseDestinationUrl? = null,
    val isBackupNotFound: Boolean = false,
    val revision: String? = null,
)
