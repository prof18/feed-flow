package com.prof18.feedflow.android.readermode

import android.webkit.CookieManager
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.ThumbVisibility
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.core.model.ThemeMode
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.utils.getArchiveISUrl
import com.prof18.feedflow.shared.utils.isValidUrl
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun ReaderModeScreen(
    readerModeState: ReaderModeState,
    fontSize: Int,
    themeMode: ThemeMode,
    onUpdateFontSize: (Int) -> Unit,
    onBookmarkClick: (FeedItemId, Boolean) -> Unit,
    navigateBack: () -> Unit,
    canNavigatePrevious: Boolean,
    canNavigateNext: Boolean,
    onNavigateToPrevious: () -> Unit,
    onNavigateToNext: () -> Unit,
    isDetailFullscreen: Boolean = false,
    onToggleDetailFullscreen: (() -> Unit)? = null,
) {
    val browserManager = koinInject<BrowserManager>()

    val context = LocalContext.current
    val navigator = rememberWebViewNavigator()
    var fullscreenImageUrl by remember { mutableStateOf<String?>(null) }
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val isAtBottom by remember {
        derivedStateOf {
            scrollState.value >= scrollState.maxValue && scrollState.maxValue > 0
        }
    }

    LaunchedEffect(isAtBottom) { if (isAtBottom) toolbarExpanded = true }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                ReaderModeToolbar(
                    navigateBack = {
                        if (navigator.canGoBack) {
                            navigator.navigateBack()
                        } else {
                            navigateBack()
                        }
                    },
                    isDetailFullscreen = isDetailFullscreen,
                    onToggleDetailFullscreen = onToggleDetailFullscreen,
                )
            },

        ) { contentPadding ->
            Box(
                modifier = Modifier,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .zIndex(zIndex = 0.5f)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
                )

                if (readerModeState !is ReaderModeState.Loading) {
                    ReaderModeFloatingToolbar(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .offset(y = -ScreenOffset)
                            .zIndex(1f)
                            .padding(
                                start = 24.dp,
                                end = 24.dp,
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        expanded = toolbarExpanded,
                        readerModeState = readerModeState,
                        fontSize = fontSize,
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
                        canNavigatePrevious = canNavigatePrevious,
                        canNavigateNext = canNavigateNext,
                        onNavigateToPrevious = onNavigateToPrevious,
                        onNavigateToNext = onNavigateToNext,
                    )
                }

                when (readerModeState) {
                    is ReaderModeState.HtmlNotAvailable -> {
                        FallbackWebView(
                            url = readerModeState.url,
                            contentPadding = contentPadding,
                            navigator = navigator,
                        )
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
                            modifier = Modifier.floatingToolbarVerticalNestedScroll(
                                expanded = toolbarExpanded,
                                onExpand = { toolbarExpanded = true },
                                onCollapse = { toolbarExpanded = false },
                            ),
                            scrollState = scrollState,
                            readerModeState = readerModeState,
                            openInBrowser = { url ->
                                if (isValidUrl(url)) {
                                    browserManager.openUrlWithFavoriteBrowser(url, context)
                                }
                            },
                            onImageClick = { imageUrl ->
                                if (imageUrl.isNotBlank()) {
                                    fullscreenImageUrl = imageUrl
                                }
                            },
                            contentPadding = contentPadding,
                            navigator = navigator,
                            themeMode = themeMode,
                        )
                    }
                }
            }
        }

        fullscreenImageUrl?.let { imageUrl ->
            FullScreenImageOverlay(
                imageUrl = imageUrl,
                onDismiss = { fullscreenImageUrl = null },
            )
        }
    }
}

@Composable
private fun FallbackWebView(
    url: String,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
) {
    val state = rememberWebViewState(url)
    WebView(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        state = state,
        navigator = navigator,
    )
}

@Composable
private fun ReaderMode(
    readerModeState: ReaderModeState.Success,
    themeMode: ThemeMode,
    openInBrowser: (String) -> Unit,
    onImageClick: (String) -> Unit,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val bodyColor = MaterialTheme.colorScheme.onSurface.toArgb().toHexString().substring(2)
    val linkColor = MaterialTheme.colorScheme.primary.toArgb().toHexString().substring(2)

    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkMode by remember {
        derivedStateOf {
            when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.OLED -> true
            }
        }
    }
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
    val latestOpenImage by rememberUpdatedState(onImageClick)

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
        jsBridge.register(
            object : IJsMessageHandler {
                override fun handle(
                    message: JsMessage,
                    navigator: WebViewNavigator?,
                    callback: (String) -> Unit,
                ) {
                    val imageUrl = message.params
                    if (imageUrl.isNotBlank() && isValidImageUrl(imageUrl)) {
                        latestOpenImage(imageUrl)
                    }
                }

                override fun methodName(): String = "imageInterceptor"
            },
        )
    }

    val state = rememberWebViewStateWithHTMLData(
        data = content,
        baseUrl = readerModeState.readerModeData.baseUrl,
    )

    val layoutDir = LocalLayoutDirection.current
    ScrollArea(
        state = rememberScrollAreaState(scrollState),
        modifier = modifier.fillMaxSize(),
    ) {
        WebView(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = contentPadding.calculateTopPadding())
                .padding(start = contentPadding.calculateLeftPadding(layoutDir))
                .padding(end = contentPadding.calculateRightPadding(layoutDir)),
            state = state,
            navigator = navigator,
            webViewJsBridge = jsBridge,
            onCreated = { webView ->
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                webView.isVerticalScrollBarEnabled = false
            },
        )

        VerticalScrollbar(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(8.dp)
                .padding(end = 2.dp),
        ) {
            Thumb(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                thumbVisibility = ThumbVisibility.HideWhileIdle(
                    hideDelay = 1500.milliseconds,
                    enter = fadeIn(tween(durationMillis = 200)),
                    exit = fadeOut(tween(durationMillis = 300)),
                ),
            )
        }
    }
}

private fun isValidImageUrl(url: String): Boolean {
    val isHttpUrl = url.startsWith("http://") || url.startsWith("https://")
    val isLocalhost = url.contains("localhost", ignoreCase = true) ||
        url.contains("127.0.0.1") ||
        url.contains("0.0.0.0") ||
        url.contains("::1")
    return isHttpUrl && !isLocalhost
}
