package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import kotlin.test.Test

class DesktopCloudSyncSuccessTest : KoinTestBase() {
    @Test
    fun `Dropbox successful workflows use real desktop worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.DROPBOX, it) }
        runQueuedCloudSuccess(CloudProvider.DROPBOX)
        runCloudFlagRegressions(CloudProvider.DROPBOX)
        runStaleCloudRefreshRegression(CloudProvider.DROPBOX)
        runCloudTransferRegressions(CloudProvider.DROPBOX)
        runCloudInvalidSnapshotRegressions(CloudProvider.DROPBOX)
        runCloudUpgradeRegressions(CloudProvider.DROPBOX)
        runCloudPendingRegressions(CloudProvider.DROPBOX)
        runCloudSnapshotRegression(CloudProvider.DROPBOX)
    }

    @Test
    fun `Drive successful workflows use real desktop worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it) }
        runQueuedCloudSuccess(CloudProvider.GOOGLE_DRIVE)
        runCloudFlagRegressions(CloudProvider.GOOGLE_DRIVE)
        runStaleCloudRefreshRegression(CloudProvider.GOOGLE_DRIVE)
        runCloudTransferRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudInvalidSnapshotRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudUpgradeRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudPendingRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudSnapshotRegression(CloudProvider.GOOGLE_DRIVE)
    }

    @Test
    fun `iCloud successful workflows use desktop bridge with local document storage`() = runTest(testDispatcher) {
        assumeTrue("iCloud is supported only on macOS", System.getProperty("os.name").contains("Mac"))
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.ICLOUD, it) }
        runQueuedCloudSuccess(CloudProvider.ICLOUD)
        runCloudFlagRegressions(CloudProvider.ICLOUD)
        runStaleCloudRefreshRegression(CloudProvider.ICLOUD)
        runCloudTransferRegressions(CloudProvider.ICLOUD)
        runCloudInvalidSnapshotRegressions(CloudProvider.ICLOUD)
        runCloudUpgradeRegressions(CloudProvider.ICLOUD)
        runCloudPendingRegressions(CloudProvider.ICLOUD)
    }
}
