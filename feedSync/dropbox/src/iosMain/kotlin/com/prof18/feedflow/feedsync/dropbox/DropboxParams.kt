package com.prof18.feedflow.feedsync.dropbox

import platform.Foundation.NSURL

actual class DropboxUploadParam(
    val path: String,
    val url: NSURL,
    val expectedRevision: String? = null,
)

actual class DropboxDownloadParam(
    val outputName: String,
    val path: String,
)
