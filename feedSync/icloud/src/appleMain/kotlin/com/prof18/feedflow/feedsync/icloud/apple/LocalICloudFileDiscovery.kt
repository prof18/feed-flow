package com.prof18.feedflow.feedsync.icloud.apple

import platform.Foundation.NSURL

/** Local fixtures opt into this boundary explicitly; production always uses [FoundationICloudFileDiscovery]. */
class LocalICloudFileDiscovery : ICloudFileDiscovery {
    override suspend fun discoverAndMaterialize(
        targetUrl: NSURL,
        timeoutMillis: Long,
    ): ICloudFileDiscoveryResult = ICloudFileDiscoveryResult.Available(targetUrl)
}
