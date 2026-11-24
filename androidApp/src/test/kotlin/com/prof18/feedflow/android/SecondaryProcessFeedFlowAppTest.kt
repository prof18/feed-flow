package com.prof18.feedflow.android

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = SecondaryProcessFeedFlowApp::class)
class SecondaryProcessFeedFlowAppTest {

    @Test
    fun secondaryProcessWithoutInternetStartsWithoutRunningAppInit() {
        ApplicationProvider.getApplicationContext<SecondaryProcessFeedFlowApp>()

        assertNull(GlobalContext.getOrNull())
    }
}

/**
 * Mirrors the WebView zygote child process seen on Android 17: the Application is created in a process
 * other than the app's own one, and that process has no INTERNET permission.
 */
class SecondaryProcessFeedFlowApp : FeedFlowApp() {
    override fun onCreate() {
        ShadowApplication.setProcessName("$packageName:sandboxed_process0")
        super.onCreate()
    }
}
