package com.prof18.feedflow.feedsync.googledrive

import android.content.Context
import co.touchlab.kermit.Logger
import com.prof18.feedflow.core.utils.DispatcherProvider

@Suppress("UnusedParameter", "UnusedPrivateProperty")
class GoogleDriveAndroidDataSourceImpl(
    context: Context,
    googleDriveSettings: GoogleDriveSettings,
    logger: Logger,
    dispatcherProvider: DispatcherProvider,
) : GoogleDriveDataSourceAndroid {
    override suspend fun isAuthorized(): Boolean = false

    override fun revokeAccess() {
        // no-op
    }

    override suspend fun performDownload(downloadParam: GoogleDriveDownloadParam): GoogleDriveDownloadResult =
        GoogleDriveDownloadResult()

    override suspend fun performUpload(uploadParam: GoogleDriveUploadParam): GoogleDriveUploadResult =
        GoogleDriveUploadResult
}
