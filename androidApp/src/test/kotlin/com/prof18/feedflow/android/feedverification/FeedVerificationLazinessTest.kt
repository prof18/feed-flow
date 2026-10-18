package com.prof18.feedflow.android.feedverification

import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import com.prof18.feedflow.android.FeedFlowApp
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.ProvideFeedFlowStrings
import com.prof18.feedflow.android.ui.utils.rememberFeedFlowStrings
import com.prof18.feedflow.core.model.FeedRecoveryRecord
import com.prof18.feedflow.core.model.RecoveryMode
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryCoordinator
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.VerificationFailure
import com.prof18.feedflow.shared.presentation.FeedRefreshVerificationViewModel
import com.prof18.feedflow.shared.presentation.PendingFeedVerification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = FeedFlowApp::class)
class FeedVerificationLazinessTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun webViewProfilesAreTouchedOnlyWhenAVerificationStarts() {
        val fake = CountingWebViewProfiles()
        val koin = GlobalContext.get()
        koin.loadModules(listOf(module { single<WebViewProfiles> { fake } }), allowOverride = true)

        compose.setContent {
            FeedFlowTheme {
                ProvideFeedFlowStrings(rememberFeedFlowStrings()) { FeedVerificationScreen() }
            }
        }
        compose.mainClock.advanceTimeBy(IDLE_MILLIS)
        compose.waitForIdle()

        assertEquals(0, fake.calls)

        val repository = koin.get<FeedRecoveryRepository>()
        val record = FeedRecoveryRecord(
            accountEpoch = repository.accountEpochState.value,
            sourceId = "source",
            subscriptionUrl = FEED_URL,
            effectiveUrl = FEED_URL,
            origin = "https://example.com:443",
            mode = RecoveryMode.VerificationRequired,
            profileId = null,
            statusCode = 403,
            retryAfter = null,
        )
        val source = com.prof18.feedflow.core.model.FeedSource(
            id = "source",
            url = FEED_URL,
            title = "Feed",
            category = null,
            lastSyncTimestamp = null,
            logoUrl = null,
            websiteUrl = null,
            fetchFailed = false,
            articleOpenMode = com.prof18.feedflow.core.model.ArticleOpenMode.DEFAULT,
            isHiddenFromTimeline = false,
            isPinned = false,
            isNotificationEnabled = false,
            isHideImagesEnabled = false,
        )
        val coordinator = koin.get<FeedRecoveryCoordinator>()
        val failures = mutableListOf<VerificationFailure>()
        val collector = CoroutineScope(Dispatchers.Unconfined).launch {
            coordinator.failures.collect { failures += it }
        }
        koin.get<FeedRefreshVerificationViewModel>().verify(PendingFeedVerification(source, record))

        repeat(WAIT_STEPS) {
            if (fake.supportedChecks == 0) {
                shadowOf(Looper.getMainLooper()).idle()
                compose.mainClock.advanceTimeBy(STEP_MILLIS)
                Thread.sleep(STEP_MILLIS)
            }
        }
        collector.cancel()

        assertTrue("visible=${coordinator.visibleState.value} failures=$failures", fake.supportedChecks > 0)
    }

    private class CountingWebViewProfiles : WebViewProfiles {
        var calls = 0
            private set
        var supportedChecks = 0
            private set

        override fun isSupported(): Boolean {
            calls++
            supportedChecks++
            return false
        }

        override fun names(): Set<String> {
            calls++
            return emptySet()
        }

        override fun defaultUserAgent(): String {
            calls++
            return "agent"
        }

        override fun engineVersion(): String {
            calls++
            return "engine"
        }

        override suspend fun clearAndDelete(profileId: String): Boolean {
            calls++
            return true
        }
    }

    private companion object {
        const val FEED_URL = "https://example.com/feed"
        const val IDLE_MILLIS = 5_000L
        const val WAIT_STEPS = 50
        const val STEP_MILLIS = 100L
    }
}
