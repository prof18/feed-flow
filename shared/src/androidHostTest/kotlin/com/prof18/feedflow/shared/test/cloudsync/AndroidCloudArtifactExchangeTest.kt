package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AndroidCloudArtifactExchangeTest : KoinTestBase() {
    @Test
    fun `dropbox snapshot exchange`() = runTest(testDispatcher) {
        runCloudArtifactExchange(CloudProvider.DROPBOX)
    }

    @Test
    fun `google drive snapshot exchange`() = runTest(testDispatcher) {
        runCloudArtifactExchange(CloudProvider.GOOGLE_DRIVE)
    }
}
