package com.prof18.feedflow.android.feedverification

import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.shared.domain.recovery.FeedRecoveryRepository
import com.prof18.feedflow.shared.domain.recovery.RecoveryOrigin
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentity
import com.prof18.feedflow.shared.domain.recovery.WebsiteIdentityProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the saved website identity of a recovery profile. It runs only when a session request
 * for a recovery record is made, so normal app use never touches the WebView profile store.
 */
internal class AndroidWebsiteIdentityProvider(
    // Lazy: the repository's own setup resolves the session adapter that depends on this provider
    repository: Lazy<FeedRecoveryRepository>,
) : WebsiteIdentityProvider {
    private val repository by repository

    override suspend fun identity(profile: FeedRecoveryProfile, url: String): WebsiteIdentity? {
        if (!profile.id.startsWith(RecoveryPolicy.PROFILE_ID_PREFIX)) return null
        if (RecoveryOrigin.of(url)?.value != profile.origin) return null
        if (!repository.isCurrent(profile)) return null
        return withContext(Dispatchers.Main.immediate) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) return@withContext null
            val store = ProfileStore.getInstance()
            if (profile.id !in store.allProfileNames) return@withContext null
            val native = store.getProfile(profile.id) ?: return@withContext null
            // The user agent the clearance was issued to: a WebView update must not invalidate the session
            WebsiteIdentity(profile.userAgent, clearanceCookie(native.cookieManager.getCookie(url)))
        }
    }

    private fun clearanceCookie(cookies: String?): String? = cookies
        ?.split(';')
        ?.map(String::trim)
        ?.singleOrNull { it.substringBefore('=') == RecoveryPolicy.REPLAY_COOKIE }
}
