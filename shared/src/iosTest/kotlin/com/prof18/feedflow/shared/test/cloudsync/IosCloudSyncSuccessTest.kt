package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class IosCloudSyncSuccessTest : KoinTestBase() {

    @Test
    fun `Dropbox successful workflows use real iOS worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.DROPBOX, it) }
        runQueuedCloudSuccess(CloudProvider.DROPBOX)
        runCloudFlagRegressions(CloudProvider.DROPBOX)
        runStaleCloudRefreshRegression(CloudProvider.DROPBOX)
        runCloudTransferRegressions(CloudProvider.DROPBOX)
        runCloudSnapshotRegression(CloudProvider.DROPBOX)
    }

    @Test
    fun `Google Drive successful workflows use real iOS worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it) }
        runQueuedCloudSuccess(CloudProvider.GOOGLE_DRIVE)
        runCloudFlagRegressions(CloudProvider.GOOGLE_DRIVE)
        runStaleCloudRefreshRegression(CloudProvider.GOOGLE_DRIVE)
        runCloudTransferRegressions(CloudProvider.GOOGLE_DRIVE)
    }

    @Test
    fun `iCloud successful workflows use real iOS worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.ICLOUD, it) }
        runQueuedCloudSuccess(CloudProvider.ICLOUD)
        runCloudFlagRegressions(CloudProvider.ICLOUD)
        runStaleCloudRefreshRegression(CloudProvider.ICLOUD)
        runCloudTransferRegressions(CloudProvider.ICLOUD)
        runCloudSnapshotRegression(CloudProvider.ICLOUD)
    }
}
