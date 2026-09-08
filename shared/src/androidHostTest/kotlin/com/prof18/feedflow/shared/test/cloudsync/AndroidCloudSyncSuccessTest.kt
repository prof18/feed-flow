package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AndroidCloudSyncSuccessTest : KoinTestBase() {
    @Test
    fun `Dropbox successful workflows use the real Android worker with host database files`() =
        runTest(testDispatcher) {
            CloudSuccessScenario.entries.forEach {
                runCloudSuccessScenario(CloudProvider.DROPBOX, it)
            }
            runCloudFlagRegressions(CloudProvider.DROPBOX)
            runStaleCloudRefreshRegression(CloudProvider.DROPBOX)
        }

    @Test
    fun `Drive successful workflows use the real Android worker with host database files`() =
        runTest(testDispatcher) {
            CloudSuccessScenario.entries.forEach {
                runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it)
            }
            runCloudFlagRegressions(CloudProvider.GOOGLE_DRIVE)
            runStaleCloudRefreshRegression(CloudProvider.GOOGLE_DRIVE)
        }
}
