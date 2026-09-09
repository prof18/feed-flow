package com.prof18.feedflow.feedsync.icloud.apple

import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSURL

private const val DEFAULT_DISCOVERY_TIMEOUT_MS = 30_000L

/**
 * Finds the exact backup URL in the app's ubiquity container and makes its current cloud version local.
 * A missing result is emitted only after the metadata query completed normally.
 */
interface ICloudFileDiscovery {
    suspend fun discoverAndMaterialize(
        targetUrl: NSURL,
        timeoutMillis: Long = DEFAULT_DISCOVERY_TIMEOUT_MS,
    ): ICloudFileDiscoveryResult
}

class FoundationICloudFileDiscovery internal constructor(
    private val metadataQuery: ICloudMetadataQuery = FoundationICloudMetadataQuery(),
    private val materializer: ICloudFileMaterializer = FoundationICloudFileMaterializer(),
) : ICloudFileDiscovery {
    override suspend fun discoverAndMaterialize(
        targetUrl: NSURL,
        timeoutMillis: Long,
    ): ICloudFileDiscoveryResult {
        if (timeoutMillis <= 0L) return ICloudFileDiscoveryResult.Failure("iCloud discovery timed out")

        return withTimeoutOrNull(timeoutMillis) {
            when (val discovery = metadataQuery.find(targetUrl)) {
                is ICloudMetadataQueryResult.Found -> {
                    when (val materialization = materializer.materialize(discovery.url)) {
                        ICloudMaterializationResult.Available -> ICloudFileDiscoveryResult.Available(discovery.url)
                        is ICloudMaterializationResult.Failure ->
                            ICloudFileDiscoveryResult.Failure(materialization.message)
                    }
                }
                ICloudMetadataQueryResult.ConfirmedMissing -> ICloudFileDiscoveryResult.ConfirmedMissing
                is ICloudMetadataQueryResult.Failure -> ICloudFileDiscoveryResult.Failure(discovery.message)
            }
        } ?: ICloudFileDiscoveryResult.Failure("iCloud discovery timed out")
    }
}

sealed class ICloudFileDiscoveryResult {
    data class Available(val url: NSURL) : ICloudFileDiscoveryResult()
    data object ConfirmedMissing : ICloudFileDiscoveryResult()
    data class Failure(val message: String) : ICloudFileDiscoveryResult()
}
