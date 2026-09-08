package com.prof18.feedflow.shared.test.cloudsync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class AndroidCloudSyncDeviceSuccessTest : KoinTestBase() {
    @Test
    fun `Dropbox successful workflows use AndroidSqliteDriver and the real Android worker`() =
        runTest(testDispatcher) {
            CloudSuccessScenario.entries.forEach {
                runCloudSuccessScenario(CloudProvider.DROPBOX, it)
            }
        }

    @Test
    fun `Drive successful workflows use AndroidSqliteDriver and the real Android worker`() =
        runTest(testDispatcher) {
            CloudSuccessScenario.entries.forEach {
                runCloudSuccessScenario(CloudProvider.GOOGLE_DRIVE, it)
            }
        }
}
