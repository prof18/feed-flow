package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderExtractor
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.ui.readermode.ReaderModeContent
import org.json.JSONException
import org.json.JSONObject
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
                    document.getElementById("__reader_container").style.fontSize = "$newFontSize" + "px";
                    document.getElementById("__reader_container").style.lineHeight = "1.5em";
                """.trimIndent(),
            )
            onUpdateFontSize(newFontSize)
        },
        openInBrowser = { url ->
            browserManager.openUrlWithFavoriteBrowser(url, context)
        },
        onShareClick = { url ->
            context.openShareSheet(
                title = null,
                url = url,
            )
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

    var isContentReady by remember {
        mutableStateOf(false)
    }
    var finalContents: String? by remember {
        mutableStateOf(null)
    }

    val isDarkMode = isSystemInDarkTheme()
    val backgroundColor = if (isDarkMode) {
        "#1e1e1e"
    } else {
        "#f6f8fa"
    }
    val borderColor = if (isDarkMode) {
        "#444444"
    } else {
        "#d1d9e0"
    }

    val colors = ReaderColors(
        textColor = "#$bodyColor",
        linkColor = "#$linkColor",
        backgroundColor = backgroundColor,
        borderColor = borderColor,
    )

    val latestOpenInBrowser by rememberUpdatedState(openInBrowser)

    Column {
        ParsingWebView(
            modifier = Modifier.requiredSize(0.dp),
            articleLink = readerModeState.readerModeData.url,
            articleContent = readerModeState.readerModeData.content,
            readerExtractor = readerModeState.readerModeData.extractor,
            contentLoaded = { result ->
                Logger.d { "Parsed article" }
                Logger.d { result }
                val jsonResult = JSONObject(result)

                val content: String
                val title: String?
                when (readerModeState.readerModeData.extractor) {
                    ReaderExtractor.POSTLIGHT -> {
                        title = jsonResult.getStringOrNull("title")
                        content = jsonResult.getStringOrNull("content").orEmpty()
                    }
                    ReaderExtractor.DEFUDDLE -> {
                        title = jsonResult.getStringOrNull("title")
                        content = jsonResult.getStringOrNull("content").orEmpty()
                    }
                }
                val finalHTML = getReaderModeStyledHtml(
                    articleLink = readerModeState.readerModeData.url,
                    colors = colors,
                    content = content,
                    title = title,
                    fontSize = readerModeState.readerModeData.fontSize,
                    heroImageUrl = readerModeState.readerModeData.heroImageUrl,
                )

                finalContents = finalHTML
                isContentReady = true
            },
        )

        if (isContentReady) {
            val jsBridge = rememberWebViewJsBridge()
            DisposableEffect(jsBridge) {
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

                onDispose { jsBridge.clear() }
            }

            val webViewState = rememberWebViewStateWithHTMLData(finalContents.orEmpty())
            webViewState.webSettings.apply {
                this.supportZoom = false
            }

            WebView(
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                state = webViewState,
                navigator = navigator,
                webViewJsBridge = jsBridge,
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

private fun JSONObject.getStringOrNull(key: String): String? =
    try {
        getString(key)
    } catch (_: JSONException) {
        null
    }
