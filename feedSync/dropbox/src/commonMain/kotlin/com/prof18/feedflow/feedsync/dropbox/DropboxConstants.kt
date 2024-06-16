package com.prof18.feedflow.feedsync.dropbox

object DropboxConstants {
    const val DROPBOX_CLIENT_IDENTIFIER = "feedflowapp"
    val DROPBOX_SCOPES = listOf("files.content.write", "files.content.read", "files.metadata.read")
}
