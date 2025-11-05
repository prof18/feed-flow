package com.prof18.feedflow.feedsync.googledrive

import java.io.File
import java.io.OutputStream

actual class GoogleDriveUploadParam(
    val path: String,
    val file: File,
)

actual class GoogleDriveDownloadParam(
    val path: String,
    val outputStream: OutputStream,
)

actual class DatabaseDestinationUrl
