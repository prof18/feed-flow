package com.prof18.feedflow.android.readermode

import android.content.Context
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
import co.touchlab.kermit.Logger
import com.dropbox.core.android.AuthActivity.Companion.result
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebContent
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.prof18.feedflow.android.BrowserManager
import com.prof18.feedflow.android.R
import com.prof18.feedflow.android.openShareSheet
import com.prof18.feedflow.core.model.FeedItemId
import com.prof18.feedflow.core.model.ReaderModeState
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtmlWithParser
import com.prof18.feedflow.shared.ui.readermode.ReaderModeContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.koin.compose.koinInject
import java.io.BufferedReader
import java.io.InputStreamReader

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
            navigateBack()
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


    val state = rememberWebViewStateWithHTMLData("")
    state.webSettings.apply {
        this.supportZoom = false
    }
val context = LocalContext.current

    LaunchedEffect(readerModeState) {
        Logger.d { ">>>>> Readermodestate" }
        withContext(Dispatchers.IO) {
            val title = readerModeState.readerModeData.title
            val readabilityJS = readRawResource(context, R.raw.readability)

            val contentHtml = getReaderModeStyledHtmlWithParser(
                colors = colors,
                readabilityJS = readabilityJS,
                title = readerModeState.readerModeData.title,
                fontSize = readerModeState.readerModeData.fontSize,
            )

            navigator.loadHtml(contentHtml)
        }
    }

    LaunchedEffect(state.loadingState) {
        withContext(Dispatchers.IO) {
            val hasHtmlTemplateLoaded = state.loadingState == LoadingState.Finished
            Logger.d { ">>> state: ${state.loadingState}" }
            Logger

            val htmlClean = JSONObject.quote(readerModeState.readerModeData.content)
            val urlClean = JSONObject.quote(readerModeState.readerModeData.url)

            val hasData = (state.content as? WebContent.Data)?.data

            if (hasHtmlTemplateLoaded) {
                Logger.d { ">>>>> html load: ${state.content}" }
                Logger.d { "Evaluate js" }
                navigator.evaluateJavaScript(
                    script = """
                        console.log('Parsing content');
                        parseContent($htmlClean, $urlClean)
                    """.trimIndent()
                ) {
                    Logger.d { ">>> GOT RESULT" }
                    Logger.d { it }

                    if (it != "null") {
                        val jsonResult = JSONObject(it)

                        val content = jsonResult.getString("content")
                        val title = jsonResult.getString("title")
                        val length = jsonResult.getInt("length")

                        // Properly quote the content for JavaScript
                        val contentQuoted = JSONObject.quote(content)
                        
                        // Update the container with the parsed content
                        navigator.evaluateJavaScript(
                            script = """
                                document.getElementById("container").innerHTML = $contentQuoted;
                                
                                 document.body.addEventListener("click", function(event) {
              if (event.target.tagName.toLowerCase() === "a") {
                  // Prevent the default behavior of the link
                  event.preventDefault();
                  var url = event.target.getAttribute("href");
                  window.kmpJsBridge.callNative(
                   "urlInterceptor", 
                    url, 
                    {}
                  );
              }
          });
                                
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }

    WebView(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        state = state,
        navigator = navigator,
        webViewJsBridge = jsBridge,
        captureBackPresses = false,
    )
}


private fun readRawResource(context: Context, resId: Int): String {
    return context.resources.openRawResource(resId).use { inputStream ->
        InputStreamReader(inputStream).use { reader ->
            BufferedReader(reader).use { bufferedReader ->
                bufferedReader.readText()
            }
        }
    }
}