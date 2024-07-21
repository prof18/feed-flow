package com.prof18.feedflow.feedsync.dropbox

import platform.Foundation.NSURL

actual class DropboxUploadParam(
    val path: String,
    val url: NSURL,
)

actual class DropboxDownloadParam(
    val outputName: String,
    val path: String,
)
