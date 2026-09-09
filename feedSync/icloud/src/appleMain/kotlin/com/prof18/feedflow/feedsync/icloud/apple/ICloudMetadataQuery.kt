package com.prof18.feedflow.feedsync.icloud.apple

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSFileManager
import platform.Foundation.NSMetadataItem
import platform.Foundation.NSMetadataItemURLKey
import platform.Foundation.NSMetadataQuery
import platform.Foundation.NSMetadataQueryDidFinishGatheringNotification
import platform.Foundation.NSMetadataQueryUbiquitousDocumentsScope
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSPredicate
import platform.Foundation.NSURL
import kotlin.coroutines.resume

internal interface ICloudMetadataQuery {
    suspend fun find(targetUrl: NSURL): ICloudMetadataQueryResult
}

internal class FoundationICloudMetadataQuery : ICloudMetadataQuery {
    override suspend fun find(targetUrl: NSURL): ICloudMetadataQueryResult =
        suspendCancellableCoroutine { continuation ->
            val query = NSMetadataQuery()
            val operationQueue = NSOperationQueue()
            operationQueue.maxConcurrentOperationCount = 1
            query.operationQueue = operationQueue
            query.searchScopes = listOf(NSMetadataQueryUbiquitousDocumentsScope)
            query.predicate = NSPredicate.predicateWithFormat(
                "kMDItemFSName == \"${metadataPredicateLiteral(requireNotNull(targetUrl.lastPathComponent))}\"",
            )

            var observer: Any? = null
            fun finish(result: ICloudMetadataQueryResult) {
                observer?.let(NSNotificationCenter.defaultCenter::removeObserver)
                if (query.started) query.stopQuery()
                if (continuation.isActive) continuation.resume(result)
            }

            observer = NSNotificationCenter.defaultCenter.addObserverForName(
                name = NSMetadataQueryDidFinishGatheringNotification,
                `object` = query,
                queue = operationQueue,
            ) {
                query.disableUpdates()
                val candidates = (0 until query.resultCount.toInt())
                    .asSequence()
                    .map { query.resultAtIndex(it.toULong()) as? NSMetadataItem }
                    .map { item ->
                        (item?.valueForAttribute(NSMetadataItemURLKey) as? NSURL)
                            ?.let(ICloudMetadataCandidate::Url)
                            ?: ICloudMetadataCandidate.Malformed
                    }
                    .toList()
                finish(
                    resolveMetadataCandidates(
                        targetUrl = targetUrl,
                        candidates = candidates,
                        targetExistsLocally = targetUrl.path
                            ?.let(NSFileManager.defaultManager::fileExistsAtPath) == true,
                    ),
                )
            }

            continuation.invokeOnCancellation {
                operationQueue.addOperationWithBlock {
                    observer?.let(NSNotificationCenter.defaultCenter::removeObserver)
                    if (query.started) query.stopQuery()
                }
            }
            operationQueue.addOperationWithBlock {
                if (!continuation.isActive) return@addOperationWithBlock
                if (!query.startQuery()) {
                    finish(ICloudMetadataQueryResult.Failure("Unable to start iCloud metadata query"))
                }
            }
        }
}

internal fun resolveMetadataCandidates(
    targetUrl: NSURL,
    candidates: List<ICloudMetadataCandidate>,
    targetExistsLocally: Boolean = false,
): ICloudMetadataQueryResult {
    candidates.filterIsInstance<ICloudMetadataCandidate.Url>()
        .firstOrNull { it.url.path == targetUrl.path }
        ?.let { return ICloudMetadataQueryResult.Found(it.url) }
    return if (candidates.any { it is ICloudMetadataCandidate.Malformed }) {
        ICloudMetadataQueryResult.Failure("iCloud metadata query returned a result without a URL")
    } else if (targetExistsLocally) {
        ICloudMetadataQueryResult.Failure("iCloud metadata query did not include the existing local backup")
    } else {
        ICloudMetadataQueryResult.ConfirmedMissing
    }
}

private fun metadataPredicateLiteral(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

internal sealed class ICloudMetadataQueryResult {
    data class Found(val url: NSURL) : ICloudMetadataQueryResult()
    data object ConfirmedMissing : ICloudMetadataQueryResult()
    data class Failure(val message: String) : ICloudMetadataQueryResult()
}

internal sealed class ICloudMetadataCandidate {
    data class Url(val url: NSURL) : ICloudMetadataCandidate()
    data object Malformed : ICloudMetadataCandidate()
}
