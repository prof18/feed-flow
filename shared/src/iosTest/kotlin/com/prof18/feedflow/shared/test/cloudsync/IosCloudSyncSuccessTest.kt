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
        runCloudInvalidSnapshotRegressions(CloudProvider.DROPBOX)
        runCloudUpgradeRegressions(CloudProvider.DROPBOX)
        runCloudPendingRegressions(CloudProvider.DROPBOX)
        runCloudFeedAndCategoryRegressions(CloudProvider.DROPBOX)
        runCloudFeedAndCategoryAccountGuardRegressions(CloudProvider.DROPBOX)
        runCloudFeedAndCategoryDeletionRegressions(CloudProvider.DROPBOX)
        runCloudFeedAndCategoryStaleBackupRegressions(CloudProvider.DROPBOX)
        runCloudSnapshotRegression(CloudProvider.DROPBOX)
        runDropboxConflictRegressions()
    }

    @Test
    fun `Google Drive successful workflows use real iOS worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it) }
        runQueuedCloudSuccess(CloudProvider.GOOGLE_DRIVE)
        runCloudFlagRegressions(CloudProvider.GOOGLE_DRIVE)
        runStaleCloudRefreshRegression(CloudProvider.GOOGLE_DRIVE)
        runCloudTransferRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudInvalidSnapshotRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudUpgradeRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudPendingRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudFeedAndCategoryRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudFeedAndCategoryAccountGuardRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudFeedAndCategoryDeletionRegressions(CloudProvider.GOOGLE_DRIVE)
        runCloudFeedAndCategoryStaleBackupRegressions(CloudProvider.GOOGLE_DRIVE)
    }

    @Test
    fun `iCloud successful workflows use real iOS worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.ICLOUD, it) }
        runICloudStagedReadRegression()
        runQueuedCloudSuccess(CloudProvider.ICLOUD)
        runCloudFlagRegressions(CloudProvider.ICLOUD)
        runStaleCloudRefreshRegression(CloudProvider.ICLOUD)
        runCloudTransferRegressions(CloudProvider.ICLOUD)
        runCloudInvalidSnapshotRegressions(CloudProvider.ICLOUD)
        runCloudUpgradeRegressions(CloudProvider.ICLOUD)
        runCloudPendingRegressions(CloudProvider.ICLOUD)
        runCloudFeedAndCategoryRegressions(CloudProvider.ICLOUD)
        runCloudFeedAndCategoryAccountGuardRegressions(CloudProvider.ICLOUD)
        runCloudFeedAndCategoryDeletionRegressions(CloudProvider.ICLOUD)
        runCloudFeedAndCategoryStaleBackupRegressions(CloudProvider.ICLOUD)
        runCloudSnapshotRegression(CloudProvider.ICLOUD)
    }
}
