package com.prof18.feedflow.android.feedverification

import android.webkit.CookieManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidWebsiteIdentityProviderTest {

    private val repository: FeedRecoveryRepository get() = GlobalContext.get().get()
    private val provider: AndroidWebsiteIdentityProvider
        get() = AndroidWebsiteIdentityProvider(lazy { repository })

    @Before
    fun requireMultiProfile() {
        assumeTrue(
            "WebView multi-profile support is required",
            onMain { WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) },
        )
    }

    @Test
    fun missingNativeProfileHasNoIdentity() = runBlocking {
        val profile = acquire(MISSING_ORIGIN_URL)
        try {
            assertNull(provider.identity(profile, MISSING_ORIGIN_URL))
        } finally {
            cleanUp(profile)
        }
    }

    @Test
    fun readerRetainedSessionServesAnotherArticleWithTheSavedUserAgent() = runBlocking {
        val profile = acquire("$URL_PREFIX/first")
        val native = onMain { ProfileStore.getInstance().getOrCreateProfile(profile.id) }
        try {
            setCookie(native.cookieManager, "$URL_PREFIX/first", "cf_clearance=fixture; Path=/; Secure")
            repository.retainForReader(profile, repository.issueTicket())

            val identity = provider.identity(profile, "$URL_PREFIX/second")

            assertEquals("cf_clearance=fixture", identity?.clearanceCookie)
            assertEquals(profile.userAgent, identity?.userAgent)
        } finally {
            cleanUp(profile)
        }
    }

    @Test
    fun clearanceIsScopedToTheProfileOriginAndPath() = runBlocking {
        val first = acquire("$URL_PREFIX/private/page")
        val other = acquire(OTHER_URL)
        val firstNative = onMain { ProfileStore.getInstance().getOrCreateProfile(first.id) }
        val otherNative = onMain { ProfileStore.getInstance().getOrCreateProfile(other.id) }
        try {
            setCookie(firstNative.cookieManager, "$URL_PREFIX/private/page", "cf_clearance=private; Path=/private")
            setCookie(firstNative.cookieManager, "$URL_PREFIX/private/page", "unrelated=secret; Path=/private")
            setCookie(otherNative.cookieManager, OTHER_URL, "cf_clearance=other; Path=/private")

            assertEquals("cf_clearance=private", provider.identity(first, "$URL_PREFIX/private/page")?.clearanceCookie)
            assertNull(provider.identity(first, "$URL_PREFIX/outside/page")?.clearanceCookie)
            assertNull(provider.identity(first, OTHER_URL))
            assertEquals("cf_clearance=other", provider.identity(other, OTHER_URL)?.clearanceCookie)
        } finally {
            cleanUp(first)
            cleanUp(other)
        }
    }

    @Test
    fun retiredProfileHasNoIdentity() = runBlocking {
        val old = acquire("$URL_PREFIX/private/page")
        val native = onMain { ProfileStore.getInstance().getOrCreateProfile(old.id) }
        try {
            setCookie(native.cookieManager, "$URL_PREFIX/private/page", "cf_clearance=old; Path=/private")
            assertEquals("cf_clearance=old", provider.identity(old, "$URL_PREFIX/private/page")?.clearanceCookie)

            repository.clearWebsite(old.id)

            assertNull(provider.identity(old, "$URL_PREFIX/private/page"))
        } finally {
            cleanUp(old)
        }
    }

    private suspend fun acquire(url: String): FeedRecoveryProfile = repository.acquireProfile(
        epoch = repository.accountEpochState.value,
        origin = requireNotNull(RecoveryOrigin.of(url)),
        userAgent = "fixture-agent",
        engineVersion = "fixture-engine",
    )

    private fun setCookie(cookieManager: CookieManager, url: String, value: String) {
        val latch = CountDownLatch(1)
        onMain { cookieManager.setCookie(url, value) { latch.countDown() } }
        check(latch.await(CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "Cookie write timed out" }
        onMain { cookieManager.flush() }
    }

    private suspend fun cleanUp(profile: FeedRecoveryProfile) {
        repository.clearWebsite(profile.id)
        onMain {
            val store = ProfileStore.getInstance()
            if (profile.id in store.allProfileNames) {
                store.getProfile(profile.id)?.let {
                    it.cookieManager.removeAllCookies(null)
                    it.cookieManager.flush()
                    it.webStorage.deleteAllData()
                }
            }
        }
    }

    private fun <T> onMain(action: () -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching(action) }
        return requireNotNull(result).getOrThrow()
    }

    private companion object {
        const val CALLBACK_TIMEOUT_SECONDS = 10L
        const val URL_PREFIX = "https://fixture.test"
        const val OTHER_URL = "https://other.fixture.test/private/page"
        const val MISSING_ORIGIN_URL = "https://missing.fixture.test/feed"
    }
}
