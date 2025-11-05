package com.prof18.feedflow.feedsync.googledrive

data class GoogleDriveUploadException(
    val errorMessage: String? = null,
    val exceptionCause: Throwable? = null,
) : Exception(errorMessage, exceptionCause)

data class GoogleDriveDownloadException(
    val errorMessage: String? = null,
    val exceptionCause: Throwable? = null,
) : Exception(errorMessage, exceptionCause)

data class GoogleDriveException(
    val causeException: Exception,
    val errorMessage: String,
) : Exception(errorMessage, causeException)
