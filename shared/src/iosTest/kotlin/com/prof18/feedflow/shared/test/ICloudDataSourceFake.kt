package com.prof18.feedflow.shared.test

import com.prof18.feedflow.feedsync.icloud.ICloudDataSource
import com.prof18.feedflow.feedsync.icloud.ICloudDownloadResult
import com.prof18.feedflow.feedsync.icloud.ICloudUploadResult
import platform.Foundation.NSURL

class ICloudDataSourceFake : ICloudDataSource {

    var iCloudBaseFolderURL: NSURL? = null
    var uploadResult: ICloudUploadResult = ICloudUploadResult.Success
    var downloadResult: ICloudDownloadResult = ICloudDownloadResult.Error.FileNotFound

    override suspend fun performUpload(databasePath: NSURL, databaseName: String): ICloudUploadResult {
        return uploadResult
    }

    override suspend fun performDownload(databaseName: String): ICloudDownloadResult {
        return downloadResult
    }

    override suspend fun getICloudBaseFolderURL(timeoutSeconds: Int, initialPollIntervalMs: Long): NSURL? {
        return iCloudBaseFolderURL
    }
}
