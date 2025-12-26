package com.prof18.feedflow.feedsync.googledrive

object GoogleDriveConstants {
    // TODO: maybe delete and use FeedFlow directly
    const val GOOGLE_DRIVE_CLIENT_APPLICATION_NAME = "FeedFlow"



    // TODO: this is probably not necessary, use
    val GOOGLE_DRIVE_SCOPES = listOf(
        "https://www.googleapis.com/auth/drive.file",
        "https://www.googleapis.com/auth/drive.appdata"
    )
}
