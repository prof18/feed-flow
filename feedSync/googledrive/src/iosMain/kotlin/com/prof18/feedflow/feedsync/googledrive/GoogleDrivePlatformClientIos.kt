package com.prof18.feedflow.feedsync.googledrive

import platform.Foundation.NSData

interface GoogleDrivePlatformClientIos {
    fun authenticate(onResult: (Boolean) -> Unit)
    fun restorePreviousSignIn(onResult: (Boolean) -> Unit)
    fun isAuthorized(): Boolean
    fun isServiceSet(): Boolean
    fun signOut()

    fun uploadFile(
        data: NSData,
        fileName: String,
        existingFileId: String?,
        completionHandler: (String?, Throwable?) -> Unit,
    )

    fun downloadFile(
        fileName: String,
        existingFileId: String?,
        completionHandler: (NSData?, Throwable?) -> Unit,
    )
}
