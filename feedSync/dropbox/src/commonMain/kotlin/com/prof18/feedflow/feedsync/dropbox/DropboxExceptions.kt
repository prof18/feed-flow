package com.prof18.feedflow.feedsync.dropbox

data class DropboxUploadException(
    val errorMessage: String? = null,
    val exceptionCause: Throwable? = null,
) : Exception(errorMessage, exceptionCause)

data class DropboxDownloadException(
    val errorMessage: String? = null,
    val exceptionCause: Throwable? = null,
) : Exception(errorMessage, exceptionCause)

data class DropboxException(
    val causeException: Exception,
    val errorMessage: String,
) : Exception(errorMessage, causeException)
