package com.prof18.feedflow.feedsync.googledrive

import platform.Foundation.NSURL

actual class GoogleDriveUploadParam(
    val path: String,
    val url: NSURL,
)

actual class GoogleDriveDownloadParam(
    val outputName: String,
    val path: String,
)

actual class DatabaseDestinationUrl(
    val url: NSURL,
)
