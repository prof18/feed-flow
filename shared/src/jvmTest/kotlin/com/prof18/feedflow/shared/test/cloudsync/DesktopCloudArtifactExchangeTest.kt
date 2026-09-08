package com.prof18.feedflow.shared.test.cloudsync

import com.prof18.feedflow.shared.test.KoinTestBase
import com.prof18.feedflow.shared.test.TestDispatcherProvider.testDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import kotlin.test.Test

class DesktopCloudArtifactExchangeTest : KoinTestBase() {
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
        assumeTrue(
            "Native iCloud is supported on macOS arm64",
            System.getProperty("os.name").contains("Mac") &&
                System.getProperty("os.arch") in listOf("aarch64", "arm64"),
        )
        runCloudArtifactExchange(CloudProvider.ICLOUD)
    }
}
