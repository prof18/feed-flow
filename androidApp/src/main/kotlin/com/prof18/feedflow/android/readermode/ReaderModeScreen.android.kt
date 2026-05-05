package com.prof18.feedflow.android.readermode

import android.webkit.CookieManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingToolbarDefaults.ScreenOffset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
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
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

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
                            onExpandToolbar = { toolbarExpanded = true },
                            onCollapseToolbar = { toolbarExpanded = false },
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
    var showPageLoader by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        showPageLoader = true
    }
    LaunchedEffect(state.loadingState) {
        showPageLoader = state.loadingState !is LoadingState.Finished
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        WebView(
            modifier = Modifier.fillMaxSize(),
            state = state,
            navigator = navigator,
        )

        if (showPageLoader) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun ReaderMode(
    readerModeState: ReaderModeState.Success,
    themeMode: ThemeMode,
    openInBrowser: (String) -> Unit,
    onImageClick: (String) -> Unit,
    contentPadding: PaddingValues,
    navigator: WebViewNavigator,
    onExpandToolbar: () -> Unit,
    onCollapseToolbar: () -> Unit,
    modifier: Modifier = Modifier,
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
    val latestExpand by rememberUpdatedState(onExpandToolbar)
    val latestCollapse by rememberUpdatedState(onCollapseToolbar)

    @Suppress("MagicNumber")
    val spacerHeightDp = (contentPadding.calculateTopPadding().value - 40f).toInt().coerceAtLeast(0)

    val content = getReaderModeStyledHtml(
        colors = colors,
        content = """
            <div id="__feedflow_top_spacer" style="height: ${spacerHeightDp}px;"></div>
            ${readerModeState.readerModeData.content}
        """.trimIndent(),
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

    val density = LocalDensity.current
    val thresholdPx = with(density) { 6.dp.toPx() }

    var scrollY by remember { mutableIntStateOf(0) }
    var scrollRange by remember { mutableIntStateOf(0) }
    var scrollExtent by remember { mutableIntStateOf(0) }
    var scrollEventCount by remember { mutableIntStateOf(0) }

    val layoutDir = LocalLayoutDirection.current
    Box(modifier = modifier.fillMaxSize()) {
        WebView(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = contentPadding.calculateLeftPadding(layoutDir))
                .padding(end = contentPadding.calculateRightPadding(layoutDir)),
            state = state,
            navigator = navigator,
            webViewJsBridge = jsBridge,
            onCreated = { webView ->
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                webView.isVerticalScrollBarEnabled = false
                webView.setOnScrollChangeListener { _, _, newScrollY, _, oldScrollY ->
                    val delta = newScrollY - oldScrollY
                    when {
                        delta.toFloat() > thresholdPx -> latestCollapse()
                        delta.toFloat() < -thresholdPx -> latestExpand()
                    }
                    scrollY = newScrollY
                    @Suppress("DEPRECATION")
                    val contentHeightPx = (webView.contentHeight * webView.scale).toInt()
                    val viewportHeightPx = webView.height
                    scrollRange = contentHeightPx
                    scrollExtent = viewportHeightPx
                    if (!webView.canScrollVertically(1)) {
                        latestExpand()
                    }
                    scrollEventCount++
                }
            },
        )

        if (scrollRange > scrollExtent) {
            ScrollbarOverlay(
                scrollY = scrollY,
                scrollRange = scrollRange,
                scrollExtent = scrollExtent,
                scrollEventCount = scrollEventCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .width(8.dp)
                    .padding(end = 2.dp),
            )
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun ScrollbarOverlay(
    scrollY: Int,
    scrollRange: Int,
    scrollExtent: Int,
    scrollEventCount: Int,
    modifier: Modifier = Modifier,
) {
    val thumbAlpha = remember { Animatable(0f) }
    LaunchedEffect(scrollEventCount) {
        if (scrollEventCount == 0) return@LaunchedEffect
        thumbAlpha.snapTo(1f)
        delay(1500)
        thumbAlpha.animateTo(0f, tween(durationMillis = 300))
    }

    BoxWithConstraints(modifier = modifier) {
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val scrollableRange = (scrollRange - scrollExtent).toFloat().coerceAtLeast(1f)
        val thumbHeightPx = (scrollExtent.toFloat() / scrollRange.toFloat()) * containerHeightPx
        val thumbMaxOffset = containerHeightPx - thumbHeightPx
        val thumbOffsetPx = (scrollY.toFloat() / scrollableRange) * thumbMaxOffset

        val thumbHeightDp = with(LocalDensity.current) { thumbHeightPx.toDp() }
        val thumbOffsetDp = with(LocalDensity.current) { thumbOffsetPx.toDp() }

        Box(
            modifier = Modifier
                .offset(y = thumbOffsetDp)
                .fillMaxWidth()
                .height(thumbHeightDp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f * thumbAlpha.value)),
        )
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
