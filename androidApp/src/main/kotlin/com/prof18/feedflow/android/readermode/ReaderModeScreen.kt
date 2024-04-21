package com.prof18.feedflow.android.readermode

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.presentation.ReaderModeState
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import org.koin.compose.koinInject

@Composable
internal fun ReaderModeScreen(
    readerModeState: ReaderModeState,
    navigateBack: () -> Unit,
) {
    val browserManager = koinInject<BrowserManager>()

    val context = LocalContext.current

    ReaderModeContent(
        readerModeState = readerModeState,
        navigateBack = navigateBack,
        openInBrowser = { url ->
            browserManager.openUrlWithFavoriteBrowser(url, context)
        },
    )
}

@Composable
private fun ReaderModeContent(
    readerModeState: ReaderModeState,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
) {
    val navigator = rememberWebViewNavigator()
    var toolbarTitle by remember {
        mutableStateOf("")
    }

    Scaffold(
        topBar = {
            ReaderModeToolbar(
                toolbarTitle = toolbarTitle,
                navigator = navigator,
                readerModeState = readerModeState,
                navigateBack = navigateBack,
                openInBrowser = openInBrowser,
            )
        },
    ) { contentPadding ->
        when (readerModeState) {
            is ReaderModeState.HtmlNotAvailable -> {
                navigateBack()
                openInBrowser(readerModeState.url)
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
                toolbarTitle = readerModeState.readerModeData.title ?: ""
                ReaderMode(
                    readerModeState = readerModeState,
                    openInBrowser = openInBrowser,
                    contentPadding = contentPadding,
                    navigator = navigator,
                )
            }
        }
    }
}

@Composable
private fun ReaderModeToolbar(
    toolbarTitle: String,
    navigator: WebViewNavigator,
    readerModeState: ReaderModeState,
    navigateBack: () -> Unit,
    openInBrowser: (String) -> Unit,
) {
    val context = LocalContext.current

    TopAppBar(
        title = {
            Text(
                text = toolbarTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier
                    .tagForTesting(TestingTag.BACK_BUTTON),
                onClick = {
                    if (navigator.canGoBack) {
                        navigator.navigateBack()
                    } else {
                        navigateBack()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        actions = {
            Row {
                if (readerModeState is ReaderModeState.Success) {
                    IconButton(
                        onClick = {
                            openInBrowser(readerModeState.readerModeData.url)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            // share
                            val shareIntent = Intent(Intent.ACTION_SEND)
                            shareIntent.setType("text/plain")
                            shareIntent.putExtra(
                                Intent.EXTRA_TEXT,
                                readerModeState.readerModeData.url,
                            )
                            context.startActivity(Intent.createChooser(shareIntent, ""))
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                        )
                    }
                }
            }
        },
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

    val content = getReaderModeHtml(
        colors = colors,
        content = readerModeState.readerModeData.content,
        title = readerModeState.readerModeData.title,
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
