package com.prof18.feedflow.android.readermode

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.multiplatform.webview.web.AccompanistWebChromeClient

/**
 * Android WebView ignores HTML5 fullscreen requests (e.g. the YouTube embed button) unless the
 * chrome client hosts the view handed to [WebChromeClient.onShowCustomView].
 */
@Composable
internal fun rememberFullscreenVideoChromeClient(): AccompanistWebChromeClient {
    var fullscreenVideo by remember { mutableStateOf<FullscreenVideo?>(null) }
    val chromeClient = remember {
        FullscreenVideoChromeClient(
            onShow = { view, callback -> fullscreenVideo = FullscreenVideo(view, callback) },
            onHide = { fullscreenVideo = null },
        )
    }

    fullscreenVideo?.let { video ->
        FullscreenVideoHost(
            video = video,
            onExit = {
                fullscreenVideo = null
                video.callback.onCustomViewHidden()
            },
        )
    }

    return chromeClient
}

@Composable
private fun FullscreenVideoHost(
    video: FullscreenVideo,
    onExit: () -> Unit,
) {
    val activity = LocalActivity.current ?: return
    BackHandler(onBack = onExit)

    DisposableEffect(video) {
        val window = activity.window
        val decorView = window.decorView as ViewGroup
        video.view.setBackgroundColor(Color.BLACK)
        decorView.addView(
            video.view,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        val controller = WindowCompat.getInsetsController(window, decorView)
        val previousBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            decorView.removeView(video.view)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}

private class FullscreenVideoChromeClient(
    private val onShow: (View, WebChromeClient.CustomViewCallback) -> Unit,
    private val onHide: () -> Unit,
) : AccompanistWebChromeClient() {
    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onShow(view, callback)
    }

    override fun onHideCustomView() {
        onHide()
    }
}

private data class FullscreenVideo(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback,
)
