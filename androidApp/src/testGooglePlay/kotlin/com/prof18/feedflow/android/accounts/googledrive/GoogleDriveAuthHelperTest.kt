package com.prof18.feedflow.android.accounts.googledrive

import androidx.activity.ComponentActivity
import androidx.credentials.exceptions.ClearCredentialProviderConfigurationException
import androidx.credentials.exceptions.ClearCredentialUnknownException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleDriveAuthHelperTest {
    @Test
    fun `disconnect continues when credential provider is unavailable`() = runTest {
        withActivity { activity ->
            val helper = GoogleDriveAuthHelper(activity) {
                throw ClearCredentialProviderConfigurationException("No provider dependencies found")
            }
            helper.performUnlink()
        }
    }

    @Test
    fun `disconnect continues when credential cleanup fails`() = runTest {
        withActivity { activity ->
            val helper = GoogleDriveAuthHelper(activity) {
                throw ClearCredentialUnknownException("Provider failed")
            }
            helper.performUnlink()
        }
    }

    @Test
    fun `disconnect clears credential state`() = runTest {
        withActivity { activity ->
            var clearCount = 0
            val helper = GoogleDriveAuthHelper(activity) { clearCount++ }
            helper.performUnlink()
            assertEquals(1, clearCount)
        }
    }

    @Test
    fun `disconnect preserves cancellation`() = runTest {
        withActivity { activity ->
            val cancellation = CancellationException("Cancelled")
            val helper = GoogleDriveAuthHelper(activity) { throw cancellation }
            val result = runCatching { helper.performUnlink() }
            assertSame(cancellation, result.exceptionOrNull())
        }
    }

    private suspend fun withActivity(block: suspend (ComponentActivity) -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            block(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
