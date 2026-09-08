package com.prof18.feedflow.shared.domain.feedsync

import app.cash.turbine.test
import com.prof18.feedflow.core.model.SyncDownloadError
import com.prof18.feedflow.core.model.SyncFeedError
import com.prof18.feedflow.core.model.SyncICloudError
import com.prof18.feedflow.core.model.SyncResult
import com.prof18.feedflow.core.model.SyncUploadError
import com.prof18.feedflow.core.utils.FeedSyncMessageQueue
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedSyncMessageQueueTest : KoinTestBase() {

    private val messageQueue: FeedSyncMessageQueue by inject()

    @Test
    fun `automatic transfer and reconciliation errors are quiet for users`() = runTest(testDispatcher) {
        messageQueue.userMessages.test {
            messageQueue.emitResult(SyncResult.General(SyncUploadError.DropboxUploadFailed))
            messageQueue.emitResult(SyncResult.General(SyncICloudError.FileAlreadyExists))
            messageQueue.emitResult(SyncResult.BackupNotFound(SyncDownloadError.DropboxDownloadFailed))
            messageQueue.emitResult(SyncResult.General(SyncFeedError.FeedSourcesSyncFailed))

            expectNoEvents()
        }
    }

    @Test
    fun `raw stream keeps every result while user stream filters notifications`() = runTest(testDispatcher) {
        val results = listOf(
            SyncResult.General(SyncUploadError.DropboxUploadFailed),
            SyncResult.GoogleDriveNeedReAuth(),
            SyncResult.Success,
            SyncResult.BackupNotFound(SyncDownloadError.DropboxDownloadFailed),
        )

        messageQueue.messageQueue.test {
            messageQueue.userMessages.test {
                results.forEach { messageQueue.emitResult(it) }

                assertEquals(results[1], awaitItem())
                expectNoEvents()
            }
            results.forEach { assertEquals(it, awaitItem()) }
        }
    }

    @Test
    fun `reauthentication is deduplicated until success despite unrelated failures`() = runTest(testDispatcher) {
        messageQueue.userMessages.test {
            val reauth = SyncResult.GoogleDriveNeedReAuth()
            messageQueue.emitResult(reauth)
            assertEquals(reauth, awaitItem())

            messageQueue.emitResult(SyncResult.General(SyncFeedError.FeedItemsSyncFailed))
            messageQueue.emitResult(SyncResult.BackupNotFound(SyncDownloadError.DropboxDownloadFailed))
            messageQueue.emitResult(SyncResult.ICloudNotAvailable(SyncICloudError.URLNotAvailable))
            messageQueue.emitResult(reauth)
            expectNoEvents()

            messageQueue.emitResult(SyncResult.Success)
            messageQueue.emitResult(reauth)
            assertEquals(reauth, awaitItem())
        }
    }

    @Test
    fun `iCloud setup unavailable is reported for every attempt but URL unavailable is quiet`() =
        runTest(testDispatcher) {
            messageQueue.userMessages.test {
                val setupUnavailable = SyncResult.ICloudNotAvailable(SyncICloudError.ServiceNotAvailable)
                messageQueue.emitResult(setupUnavailable)
                assertEquals(setupUnavailable, awaitItem())
                messageQueue.emitResult(setupUnavailable)
                assertEquals(setupUnavailable, awaitItem())

                messageQueue.emitResult(SyncResult.ICloudNotAvailable(SyncICloudError.URLNotAvailable))
                expectNoEvents()
            }
        }
}
