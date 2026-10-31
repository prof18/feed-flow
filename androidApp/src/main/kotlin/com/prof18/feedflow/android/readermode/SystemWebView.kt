package com.prof18.feedflow.android.readermode

import android.util.AndroidRuntimeException
import android.webkit.CookieManager

/**
 * False when the system WebView package is disabled or missing. Every WebView, CookieManager or
 * WebSettings call then throws, so the reader, which renders in a WebView, can't be shown.
 */
internal fun isSystemWebViewAvailable(): Boolean = try {
    // Loads the WebView provider, the same step the reader's WebView would take first.
    CookieManager.getInstance()
    true
} catch (_: AndroidRuntimeException) {
    false
}
