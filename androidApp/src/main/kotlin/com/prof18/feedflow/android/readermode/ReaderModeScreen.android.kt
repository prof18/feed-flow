package com.prof18.feedflow.android.readermode

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.ui.readermode.ReaderModeContent
import org.koin.compose.koinInject

@Composable
internal fun ReaderModeScreen(
    readerModeState: ReaderModeState,
    fontSize: Int,
    onUpdateFontSize: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    navigateBack: () -> Unit,
) {
    val browserManager = koinInject<BrowserManager>()

    val context = LocalContext.current
    val navigator = rememberWebViewNavigator()

    ReaderModeContent(
        readerModeState = readerModeState,
        fontSize = fontSize,
        navigateBack = {
            if (navigator.canGoBack) {
                navigator.navigateBack()
            } else {
                navigateBack()
            }
        },
        onFontSizeChange = { newFontSize ->
            navigator.evaluateJavaScript(
                """
                    document.getElementById("container").style.fontSize = "$newFontSize" + "px";
                    document.getElementById("container").style.lineHeight = "1.5em";
                """.trimIndent(),
            )
            onUpdateFontSize(newFontSize)
        },
        openInBrowser = { url ->
            browserManager.openUrlWithFavoriteBrowser(url, context)
        },
        onShareClick = { url ->
            // share
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.setType("text/plain")
            shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                url,
            )
            context.startActivity(Intent.createChooser(shareIntent, ""))
        },
        readerModeSuccessView = { contentPadding, state ->
            ReaderMode(
                readerModeState = state,
                openInBrowser = { url ->
                    browserManager.openUrlWithFavoriteBrowser(url, context)
                },
                contentPadding = contentPadding,
                navigator = navigator,
            )
        },
        onBookmarkClick = onBookmarkClick,
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun ReaderMode(
    readerModeState: ReaderModeState.Success,
    openInBrowser: (String) -> Unit,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
) {
    val bodyColor = MaterialTheme.colorScheme.onSurface.toArgb().toHexString().substring(2)
    val linkColor = MaterialTheme.colorScheme.primary.toArgb().toHexString().substring(2)

    val colors = ReaderColors(
        textColor = "#$bodyColor",
        linkColor = "#$linkColor",
    )

    val latestOpenInBrowser by rememberUpdatedState(openInBrowser)

    val content = getReaderModeStyledHtml(
        colors = colors,
        content = readerModeState.readerModeData.content,
        title = readerModeState.readerModeData.title,
        fontSize = readerModeState.readerModeData.fontSize,
    )

    val jsBridge = rememberWebViewJsBridge()
    LaunchedEffect(jsBridge) {
        jsBridge.register(
            object : IJsMessageHandler {
                override fun handle(
                    message: JsMessage,
                    navigator: WebViewNavigator?,
                    callback: (String) -> Unit,
                ) {
                    if (message.params.isNotBlank()) {
                        latestOpenInBrowser(message.params)
                    }
                }

                override fun methodName(): String = "urlInterceptor"
            },
        )
    }

    val state = rememberWebViewStateWithHTMLData(content)

    WebView(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        state = state,
        navigator = navigator,
        webViewJsBridge = jsBridge,
    )
}
