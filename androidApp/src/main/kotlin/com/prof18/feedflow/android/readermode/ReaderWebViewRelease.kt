package com.prof18.feedflow.android.readermode

import android.webkit.WebView

/**
 * compose-webview leaves the Android WebView alive when it leaves composition, so a video in the
 * page keeps playing and buffering after the reader closes. Tear it down instead.
 */
internal fun WebView.releaseReaderWebView() {
    stopLoading()
    onPause()
    destroy()
}
