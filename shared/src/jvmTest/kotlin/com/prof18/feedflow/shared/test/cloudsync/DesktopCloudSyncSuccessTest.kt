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
    }

    @Test
    fun `Drive successful workflows use real desktop worker and database files`() = runTest(testDispatcher) {
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it) }
        runQueuedCloudSuccess(CloudProvider.GOOGLE_DRIVE)
    }

    @Test
    fun `iCloud successful workflows use desktop bridge with local document storage`() = runTest(testDispatcher) {
        assumeTrue("iCloud is supported only on macOS", System.getProperty("os.name").contains("Mac"))
        CloudSuccessScenario.entries.forEach { runCloudSuccessScenario(CloudProvider.ICLOUD, it) }
        runQueuedCloudSuccess(CloudProvider.ICLOUD)
    }
}
