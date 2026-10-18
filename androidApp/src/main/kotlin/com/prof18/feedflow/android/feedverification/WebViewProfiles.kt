package com.prof18.feedflow.android.feedverification

import android.content.Context
import android.webkit.WebSettings
import androidx.webkit.ProfileStore
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Every call loads the WebView provider: callers run it only when a verification or a cleanup starts. */
internal interface WebViewProfiles {
    fun isSupported(): Boolean
    fun names(): Set<String>
    fun defaultUserAgent(): String
    fun engineVersion(): String

    /** Clears cookies and browsing data, then deletes the profile; false when it is loaded in this process. */
    suspend fun clearAndDelete(profileId: String): Boolean
}

internal class AndroidWebViewProfiles(private val context: Context) : WebViewProfiles {

    override fun isSupported(): Boolean = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
        WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

    override fun names(): Set<String> = ProfileStore.getInstance().allProfileNames.toSet()

    override fun defaultUserAgent(): String = WebSettings.getDefaultUserAgent(context)

    override fun engineVersion(): String = WebViewCompat.getCurrentWebViewPackage(context)?.versionName.orEmpty()

    override suspend fun clearAndDelete(profileId: String): Boolean {
        val store = ProfileStore.getInstance()
        if (profileId !in store.allProfileNames) return true
        store.getProfile(profileId)?.let { profile ->
            suspendCancellableCoroutine { continuation ->
                profile.cookieManager.removeAllCookies {
                    profile.cookieManager.flush()
                    WebStorageCompat.deleteBrowsingData(profile.webStorage) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
        return try {
            store.deleteProfile(profileId)
            true
        } catch (_: IllegalStateException) {
            false
        }
    }
}
