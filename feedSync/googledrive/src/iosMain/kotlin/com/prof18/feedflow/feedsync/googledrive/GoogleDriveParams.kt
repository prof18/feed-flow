package com.prof18.feedflow.feedsync.googledrive

import platform.Foundation.NSURL

actual class GoogleDriveUploadParam(
    val fileName: String,
    val url: NSURL,
)

actual class GoogleDriveDownloadParam(
    val fileName: String,
    val outputName: String,
)

actual class DatabaseDestinationUrl(
    val url: NSURL,
)
