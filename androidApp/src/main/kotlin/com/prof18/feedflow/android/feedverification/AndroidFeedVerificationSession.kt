package com.prof18.feedflow.android.feedverification

import android.content.Context
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProfileStore
import androidx.webkit.WebStorageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.prof18.feedflow.core.model.FeedRecoveryProfile
import com.prof18.feedflow.core.utils.HttpHostValidator
import com.prof18.feedflow.shared.domain.recovery.BrowserCapture
import com.prof18.feedflow.shared.domain.recovery.RecoveryPolicy
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URI
import kotlin.coroutines.resume

internal class AndroidFeedVerificationSession(
    context: Context,
    val metadata: FeedRecoveryProfile,
    onNavigation: () -> Unit,
    onRendererStopped: () -> Unit,
    onPageReady: () -> Unit = {},
) {
    init {
        check(Looper.myLooper() == Looper.getMainLooper())
        check(isSupported())
        require(metadata.id.startsWith(RecoveryPolicy.PROFILE_ID_PREFIX) && !metadata.retired)
    }
    private val profile = ProfileStore.getInstance().getOrCreateProfile(metadata.id)
    private var closed = false
    val webView = WebView(context).also { view ->
        WebViewCompat.setProfile(view, metadata.id)
        view.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        profile.cookieManager.setAcceptThirdPartyCookies(view, false)
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.isForMainFrame &&
                    (!safeUrl(request.url.toString()) || origin(request.url.toString()) != metadata.origin)

            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                onNavigation()
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!closed) onPageReady()
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                if (!closed) {
                    close()
                    onRendererStopped()
                }
                return true
            }
        }
    }
    private val transfer = AndroidBrowserTransfer(webView)

    fun open(url: String) {
        check(!closed && safeUrl(url) && origin(url) == metadata.origin)
        webView.loadUrl(url)
    }

    suspend fun capture(url: String): BrowserCapture {
        check(!closed)
        return transfer.capture(url)
    }

    suspend fun clear() = suspendCancellableCoroutine { continuation ->
        profile.cookieManager.removeAllCookies {
            profile.cookieManager.flush()
            WebStorageCompat.deleteBrowsingData(profile.webStorage) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        profile.cookieManager.flush()
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
    }

    companion object {
        fun isSupported(): Boolean = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)

        private fun safeUrl(url: String): Boolean = HttpHostValidator.isUrlSafeForHttpClient(url) &&
            runCatching { URI(url).let { it.scheme == "https" && it.userInfo == null } }.getOrDefault(false)

        private fun origin(url: String): String? = runCatching {
            URI(url).let { "${it.scheme}://${it.host.lowercase()}:${if (it.port == -1) HTTPS_PORT else it.port}" }
        }.getOrNull()

        private const val HTTPS_PORT = 443
    }
}
