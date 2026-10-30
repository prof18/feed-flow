package com.prof18.feedflow.android.feedverification

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.core.app.ActivityScenario
import com.prof18.feedflow.android.e2e.FeedVerificationTestActivity
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.domain.feeditem.FeedItemContentFileHandler
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.WebsiteProfileCleaner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AndroidFeedVerificationSessionTest {

    @Test
    fun productionTransferCapturesBytesFromSameOriginWebView() {
        assumeTrue("WebView multi-profile support is required", AndroidFeedVerificationSession.isSupported())
        val pageLoaded = CountDownLatch(1)
        val transferSession = AtomicReference<AndroidFeedVerificationSession?>()
        val body = byteArrayOf(
            0x00,
            DASH_BYTE.toByte(),
            DELETE_BYTE.toByte(),
            UTF8_E_ACUTE_FIRST_BYTE.toByte(),
            UTF8_E_ACUTE_SECOND_BYTE.toByte(),
            SIGNED_BYTE.toByte(),
        )

        ActivityScenario.launch(FeedVerificationTestActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    val session = newSession(activity) {}
                    transferSession.set(session)
                    val browser = session.webView
                    browser.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView,
                            request: WebResourceRequest,
                        ): WebResourceResponse {
                            val isFeed = request.url.toString() == FEED_URL
                            return WebResourceResponse(
                                if (isFeed) "application/octet-stream" else "text/html",
                                "UTF-8",
                                HTTP_OK,
                                "OK",
                                emptyMap(),
                                ByteArrayInputStream(if (isFeed) body else PAGE_HTML),
                            )
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            if (url == PAGE_URL) pageLoaded.countDown()
                        }
                    }
                    attach(activity, browser)
                    session.open(PAGE_URL)
                }

                assertTrue(
                    "Fixture page did not load",
                    pageLoaded.await(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                )
                val session = requireNotNull(transferSession.get())
                val captured = runBlocking { session.capture(FEED_URL) }

                assertEquals(FEED_URL, captured.effectiveUrl)
                assertEquals(HTTP_OK, captured.statusCode)
                assertArrayEquals(body, captured.bytes)
            } finally {
                scenario.onActivity { transferSession.get()?.close() }
            }
        }
    }

    @Test
    fun rendererExitClosesSessionAndANewSameOriginProfileRemainsUsable() {
        assumeTrue("WebView multi-profile support is required", AndroidFeedVerificationSession.isSupported())
        assumeTrue(
            "WebView renderer termination requires Android 10+",
            android.os.Build.VERSION.SDK_INT >= ANDROID_Q_API,
        )
        val rendererStopped = CountDownLatch(1)
        val firstPageReady = CountDownLatch(1)
        val secondPageReady = CountDownLatch(1)
        val rendererAvailable = AtomicReference(false)
        val rendererTerminated = AtomicReference(false)
        val secondPageResult = AtomicReference<String?>()
        val firstSession = AtomicReference<AndroidFeedVerificationSession?>()
        val nextSession = AtomicReference<AndroidFeedVerificationSession?>()

        ActivityScenario.launch(FeedVerificationTestActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    val session = newSession(activity) { rendererStopped.countDown() }
                    firstSession.set(session)
                    attach(activity, session.webView)
                    session.webView.loadDataWithBaseURL(PAGE_URL, FIRST_PAGE_HTML, "text/html", "UTF-8", null)
                    session.webView.postVisualStateCallback(
                        VISUAL_STATE_REQUEST,
                        object : WebView.VisualStateCallback() {
                            override fun onComplete(requestId: Long) {
                                firstPageReady.countDown()
                            }
                        },
                    )
                }

                assertTrue(
                    "Initial same-origin fixture did not render",
                    firstPageReady.await(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                )
                val session = requireNotNull(firstSession.get())
                scenario.onActivity {
                    val renderer = session.webView.webViewRenderProcess
                    rendererAvailable.set(renderer != null)
                    rendererTerminated.set(renderer?.terminate() == true)
                }
                assertTrue("The fixture did not start a WebView renderer", rendererAvailable.get())
                assertTrue("WebView renderer could not be terminated", rendererTerminated.get())
                assertTrue(
                    "Production renderer callback did not run",
                    rendererStopped.await(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                )

                scenario.onActivity { activity ->
                    val replacement = newSession(activity) {}
                    nextSession.set(replacement)
                    attach(activity, replacement.webView)
                    replacement.webView.loadDataWithBaseURL(PAGE_URL, SECOND_PAGE_HTML, "text/html", "UTF-8", null)
                    replacement.webView.postVisualStateCallback(
                        VISUAL_STATE_REQUEST,
                        object : WebView.VisualStateCallback() {
                            override fun onComplete(requestId: Long) {
                                replacement.webView.evaluateJavascript(
                                    "'ready:' + document.body.textContent",
                                ) { value ->
                                    secondPageResult.set(value)
                                    secondPageReady.countDown()
                                }
                            }
                        },
                    )
                }

                assertTrue(
                    "New session's same-origin page did not execute JavaScript",
                    secondPageReady.await(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                )
                assertTrue(
                    requireNotNull(secondPageResult.get()).contains("ready:New profile is usable"),
                )
            } finally {
                scenario.onActivity {
                    firstSession.get()?.close()
                    nextSession.get()?.close()
                }
            }
        }
    }

    @Test
    fun stagedReaderCacheReplacementAndRejectedCommitPreserveOldFile() = runBlocking {
        val handler = GlobalContext.get().get<FeedItemContentFileHandler>()
        val articleId = "recovery-device-fixture-${UUID.randomUUID()}"
        try {
            handler.saveFeedItemContentToFile(articleId, "old article")
            val rejected = requireNotNull(handler.stageContent(articleId, "rejected article"))
            assertFalse(handler.promoteStaged(rejected) { error("stale owner") })
            handler.discardStaged(rejected)
            assertEquals("old article", handler.loadFeedItemContent(articleId))

            val accepted = requireNotNull(handler.stageContent(articleId, "new article"))
            assertTrue(handler.promoteStaged(accepted) { it() })
            assertEquals("new article", handler.loadFeedItemContent(articleId))
        } finally {
            handler.deleteFeedItemContent(articleId)
        }
    }

    @Test
    fun clearedWebsiteProfileIsRetiredAndCleanedThroughTheCleaner() = runBlocking {
        val webViewProfiles = GlobalContext.get().get<WebViewProfiles>()
        assumeTrue("WebView multi-profile support is required", onMain { webViewProfiles.isSupported() })
        val repository = GlobalContext.get().get<FeedRecoveryRepository>()
        val cleaner = GlobalContext.get().get<WebsiteProfileCleaner>()
        val profile = repository.acquireProfile(
            epoch = repository.accountEpochState.value,
            origin = requireNotNull(RecoveryOrigin.of(PAGE_URL)),
            userAgent = "android-test-agent",
            engineVersion = "android-test-engine",
        )

        repository.clearWebsite(profile.id)
        cleaner.clean()

        val stored = repository.profile(profile.id)
        assertTrue(stored == null || stored.retired)
        assertFalse(onMain { profile.id in webViewProfiles.names() })
    }

    private fun newSession(
        context: FeedVerificationTestActivity,
        onRendererStopped: () -> Unit,
    ) = AndroidFeedVerificationSession(
        context = context,
        metadata = FeedRecoveryProfile(
            id = "feedflow-recovery-${UUID.randomUUID()}",
            accountEpoch = "android-test-epoch",
            origin = ORIGIN,
            userAgent = "android-test-agent",
            engineVersion = "android-test-engine",
            readerRetained = false,
            retired = false,
        ),
        onNavigation = {},
        onRendererStopped = onRendererStopped,
    )

    private fun <T> onMain(action: () -> T): T {
        var result: Result<T>? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .runOnMainSync { result = runCatching(action) }
        return requireNotNull(result).getOrThrow()
    }

    private fun attach(activity: FeedVerificationTestActivity, browser: WebView) {
        activity.addContentView(
            browser,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
    }

    private companion object {
        const val HTTP_OK = 200
        const val ORIGIN = "https://fixture.test:443"
        const val PAGE_URL = "https://fixture.test/"
        const val FEED_URL = "https://fixture.test/feed?token=production-transfer"
        const val LOAD_TIMEOUT_SECONDS = 15L
        const val VISUAL_STATE_REQUEST = 1L
        const val ANDROID_Q_API = 29
        const val DASH_BYTE = 0x2d
        const val DELETE_BYTE = 0x7f
        const val UTF8_E_ACUTE_FIRST_BYTE = 0xc3
        const val UTF8_E_ACUTE_SECOND_BYTE = 0xa9
        const val SIGNED_BYTE = 0xff
        val PAGE_HTML = "<html><body>Production transfer fixture</body></html>".encodeToByteArray()
        const val FIRST_PAGE_HTML = "<html><body>Renderer termination fixture</body></html>"
        const val SECOND_PAGE_HTML = "<html><body>New profile is usable</body></html>"
    }
}
