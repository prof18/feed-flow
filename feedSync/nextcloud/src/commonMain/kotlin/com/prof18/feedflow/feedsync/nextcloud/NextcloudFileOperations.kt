package com.prof18.feedflow.feedsync.nextcloud

internal interface NextcloudFileOperations {
    fun readFile(filePath: String): ByteArray
    fun writeFile(filePath: String, content: ByteArray)
}

internal expect class NextcloudFileOperationsImpl() : NextcloudFileOperations
