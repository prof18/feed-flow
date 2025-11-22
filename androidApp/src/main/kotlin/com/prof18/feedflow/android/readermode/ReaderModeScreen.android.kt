package com.prof18.feedflow.android.readermode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.utils.getArchiveISUrl
import com.prof18.feedflow.shared.utils.isValidUrl
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

    Scaffold(
        topBar = {
            ReaderModeToolbar(
                readerModeState = readerModeState,
                fontSize = fontSize,
                navigateBack = {
                    if (navigator.canGoBack) {
                        navigator.navigateBack()
                    } else {
                        navigateBack()
                    }
                },
                openInBrowser = { url ->
                    if (isValidUrl(url)) {
                        browserManager.openUrlWithFavoriteBrowser(url, context)
                    }
                },
                onShareClick = { url, title ->
                    context.openShareSheet(
                        title = title,
                        url = url,
                    )
                },
                onArchiveClick = { articleUrl ->
                    val archiveUrl = getArchiveISUrl(articleUrl)
                    if (isValidUrl(archiveUrl)) {
                        browserManager.openUrlWithFavoriteBrowser(archiveUrl, context)
                    }
                },
                onCommentsClick = { commentsUrl ->
                    if (isValidUrl(commentsUrl)) {
                        browserManager.openUrlWithFavoriteBrowser(commentsUrl, context)
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
                onBookmarkClick = onBookmarkClick,
            )
        },
    ) { contentPadding ->
        when (readerModeState) {
            is ReaderModeState.HtmlNotAvailable -> {
                navigateBack()
                if (isValidUrl(readerModeState.url)) {
                    browserManager.openUrlWithFavoriteBrowser(readerModeState.url, context)
                }
            }
            ReaderModeState.Loading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    CircularProgressIndicator()
                }
            }
            is ReaderModeState.Success -> {
                ReaderMode(
                    readerModeState = readerModeState,
                    openInBrowser = { url ->
                        if (isValidUrl(url)) {
                            browserManager.openUrlWithFavoriteBrowser(url, context)
                        }
                    },
                    contentPadding = contentPadding,
                    navigator = navigator,
                )
            }
        }
    }
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

    val content = getReaderModeStyledHtml(
        colors = colors,
        content = readerModeState.readerModeData.content,
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

    val layoutDir = LocalLayoutDirection.current
    WebView(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
            .padding(start = contentPadding.calculateLeftPadding(layoutDir))
            .padding(end = contentPadding.calculateRightPadding(layoutDir)),
        state = state,
        navigator = navigator,
        webViewJsBridge = jsBridge,
    )
}
