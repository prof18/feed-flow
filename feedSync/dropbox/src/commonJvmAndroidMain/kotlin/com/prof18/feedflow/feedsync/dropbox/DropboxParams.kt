package com.prof18.feedflow.feedsync.dropbox

import java.io.File
import java.io.OutputStream

actual class DropboxUploadParam(
    val path: String,
    val file: File,
)

actual class DropboxDownloadParam(
    val path: String,
    val outputStream: OutputStream,
)
