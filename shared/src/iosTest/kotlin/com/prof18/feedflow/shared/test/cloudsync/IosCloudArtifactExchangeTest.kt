package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class IosCloudArtifactExchangeTest : KoinTestBase() {
    @Test
    fun `dropbox snapshot exchange`() = runTest(testDispatcher) {
        runCloudArtifactExchange(CloudProvider.DROPBOX)
    }

    @Test
    fun `google drive snapshot exchange`() = runTest(testDispatcher) {
        runCloudArtifactExchange(CloudProvider.GOOGLE_DRIVE)
    }

    @Test
    fun `icloud snapshot exchange`() = runTest(testDispatcher) {
        runCloudArtifactExchange(CloudProvider.ICLOUD)
    }
}
