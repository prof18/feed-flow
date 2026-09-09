package com.prof18.feedflow.feedsync.icloud.apple

import kotlinx.coroutines.delay
import platform.Foundation.NSURL

private const val MATERIALIZATION_POLL_INTERVAL_MS = 250L

internal interface ICloudFileMaterializer {
    suspend fun materialize(url: NSURL): ICloudMaterializationResult
}

internal class FoundationICloudFileMaterializer(
    private val ubiquityDownload: ICloudUbiquityDownload = FoundationICloudUbiquityDownload(),
    private val pollIntervalMillis: Long = MATERIALIZATION_POLL_INTERVAL_MS,
) : ICloudFileMaterializer {
    override suspend fun materialize(url: NSURL): ICloudMaterializationResult {
        val initialStatus = ubiquityDownload.status(url)
        if (initialStatus is ICloudUbiquityDownloadStatus.Failure) return initialStatus.toFailure()
        if (initialStatus == ICloudUbiquityDownloadStatus.Current) return ICloudMaterializationResult.Available

        val startFailure = ubiquityDownload.requestDownload(url)
        if (startFailure != null) {
            return ICloudMaterializationResult.Failure("Unable to download iCloud file: $startFailure")
        }

        while (true) {
            when (val status = ubiquityDownload.status(url)) {
                ICloudUbiquityDownloadStatus.Current -> return ICloudMaterializationResult.Available
                is ICloudUbiquityDownloadStatus.Failure -> return status.toFailure()
                ICloudUbiquityDownloadStatus.Pending -> delay(pollIntervalMillis)
            }
        }
    }
}

private fun ICloudUbiquityDownloadStatus.Failure.toFailure(): ICloudMaterializationResult.Failure =
    ICloudMaterializationResult.Failure("Unable to read iCloud download status: $message")

internal sealed class ICloudMaterializationResult {
    data object Available : ICloudMaterializationResult()
    data class Failure(val message: String) : ICloudMaterializationResult()
}
