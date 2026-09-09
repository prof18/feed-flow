package com.prof18.feedflow.feedsync.icloud

import com.prof18.feedflow.feedsync.icloud.apple.FoundationICloudFileDiscovery
import com.prof18.feedflow.feedsync.icloud.apple.FoundationICloudFileMaterializer
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileDiscoveryResult
import com.prof18.feedflow.feedsync.icloud.apple.ICloudFileMaterializer
import com.prof18.feedflow.feedsync.icloud.apple.ICloudMaterializationResult
import com.prof18.feedflow.feedsync.icloud.apple.ICloudMetadataCandidate
import com.prof18.feedflow.feedsync.icloud.apple.ICloudMetadataQuery
import com.prof18.feedflow.feedsync.icloud.apple.ICloudMetadataQueryResult
import com.prof18.feedflow.feedsync.icloud.apple.ICloudUbiquityDownload
import com.prof18.feedflow.feedsync.icloud.apple.ICloudUbiquityDownloadStatus
import com.prof18.feedflow.feedsync.icloud.apple.resolveMetadataCandidates
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ICloudFileDiscoveryTest {
    private val targetUrl = NSURL.fileURLWithPath("/Documents/backup.db")

    @Test
    fun `found metadata materializes the exact remote URL`() = runBlocking {
        val materializer = FakeMaterializer(ICloudMaterializationResult.Available)
        val result = FoundationICloudFileDiscovery(
            metadataQuery = FakeMetadataQuery(ICloudMetadataQueryResult.Found(targetUrl)),
            materializer = materializer,
        ).discoverAndMaterialize(targetUrl)

        assertEquals(targetUrl, assertIs<ICloudFileDiscoveryResult.Available>(result).url)
        assertEquals(targetUrl, materializer.materializedUrl)
    }

    @Test
    fun `completed metadata query with no exact match confirms absence`() = runBlocking {
        val materializer = FakeMaterializer(ICloudMaterializationResult.Available)
        val result = FoundationICloudFileDiscovery(
            metadataQuery = FakeMetadataQuery(ICloudMetadataQueryResult.ConfirmedMissing),
            materializer = materializer,
        ).discoverAndMaterialize(targetUrl)

        assertIs<ICloudFileDiscoveryResult.ConfirmedMissing>(result)
        assertEquals(null, materializer.materializedUrl)
    }

    @Test
    fun `materialization failure never confirms absence`() = runBlocking {
        val result = FoundationICloudFileDiscovery(
            metadataQuery = FakeMetadataQuery(ICloudMetadataQueryResult.Found(targetUrl)),
            materializer = FakeMaterializer(ICloudMaterializationResult.Failure("download failed")),
        ).discoverAndMaterialize(targetUrl)

        assertIs<ICloudFileDiscoveryResult.Failure>(result)
        Unit
    }

    @Test
    fun `timed out discovery never confirms absence`() = runBlocking {
        val result = FoundationICloudFileDiscovery(
            metadataQuery = object : ICloudMetadataQuery {
                override suspend fun find(targetUrl: NSURL): ICloudMetadataQueryResult = awaitCancellation()
            },
            materializer = FakeMaterializer(ICloudMaterializationResult.Available),
        ).discoverAndMaterialize(targetUrl, timeoutMillis = 1L)

        assertIs<ICloudFileDiscoveryResult.Failure>(result)
        Unit
    }

    @Test
    fun `metadata result from another path does not replace the requested backup`() {
        val result = resolveMetadataCandidates(
            targetUrl,
            listOf(ICloudMetadataCandidate.Url(NSURL.fileURLWithPath("/Documents/other/backup.db"))),
        )

        assertIs<ICloudMetadataQueryResult.ConfirmedMissing>(result)
    }

    @Test
    fun `metadata result without URL is a failure not absence`() {
        val result = resolveMetadataCandidates(targetUrl, listOf(ICloudMetadataCandidate.Malformed))

        assertIs<ICloudMetadataQueryResult.Failure>(result)
    }

    @Test
    fun `empty metadata while target exists locally is a failure not absence`() {
        val result = resolveMetadataCandidates(targetUrl, emptyList(), targetExistsLocally = true)

        assertIs<ICloudMetadataQueryResult.Failure>(result)
    }

    @Test
    fun `pending remote file requests download and waits for current status`() = runBlocking {
        val download = FakeUbiquityDownload(
            statuses = ArrayDeque(
                listOf(ICloudUbiquityDownloadStatus.Pending, ICloudUbiquityDownloadStatus.Current),
            ),
        )
        val result = FoundationICloudFileMaterializer(download, pollIntervalMillis = 0L).materialize(targetUrl)

        assertIs<ICloudMaterializationResult.Available>(result)
        assertEquals(1, download.requests)
    }

    @Test
    fun `current remote file avoids another download request`() = runBlocking {
        val download = FakeUbiquityDownload(ArrayDeque(listOf(ICloudUbiquityDownloadStatus.Current)))
        val result = FoundationICloudFileMaterializer(download).materialize(targetUrl)

        assertIs<ICloudMaterializationResult.Available>(result)
        assertEquals(0, download.requests)
    }

    @Test
    fun `rejected materialization request is a failure`() = runBlocking {
        val download = FakeUbiquityDownload(
            statuses = ArrayDeque(listOf(ICloudUbiquityDownloadStatus.Pending)),
            requestFailure = "request rejected",
        )

        assertIs<ICloudMaterializationResult.Failure>(FoundationICloudFileMaterializer(download).materialize(targetUrl))
        Unit
    }

    @Test
    fun `materialization timeout cancels its pending wait`() = runBlocking {
        val download = FakeUbiquityDownload(
            statuses = ArrayDeque(List(100) { ICloudUbiquityDownloadStatus.Pending }),
        )
        val result = withTimeoutOrNull(1L) {
            FoundationICloudFileMaterializer(download, pollIntervalMillis = 10L).materialize(targetUrl)
        }

        assertEquals(null, result)
    }
}

private class FakeMetadataQuery(
    private val result: ICloudMetadataQueryResult,
) : ICloudMetadataQuery {
    override suspend fun find(targetUrl: NSURL): ICloudMetadataQueryResult = result
}

private class FakeMaterializer(
    private val result: ICloudMaterializationResult,
) : ICloudFileMaterializer {
    var materializedUrl: NSURL? = null
        private set

    override suspend fun materialize(url: NSURL): ICloudMaterializationResult {
        materializedUrl = url
        return result
    }
}

private class FakeUbiquityDownload(
    private val statuses: ArrayDeque<ICloudUbiquityDownloadStatus>,
    private val requestFailure: String? = null,
) : ICloudUbiquityDownload {
    var requests = 0
        private set

    override fun status(url: NSURL): ICloudUbiquityDownloadStatus =
        statuses.removeFirstOrNull() ?: ICloudUbiquityDownloadStatus.Pending

    override fun requestDownload(url: NSURL): String? {
        requests += 1
        return requestFailure
    }
}
