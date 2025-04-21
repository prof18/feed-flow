package com.prof18.feedflow.android.readermode

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.prof18.feedflow.android.R
import com.prof18.feedflow.core.model.ReaderExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ParsingWebView(
    articleLink: String,
    articleContent: String,
    readerExtractor: ReaderExtractor,
    contentLoaded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var html by remember(articleLink) { mutableStateOf("") }

    LaunchedEffect(articleLink) {
        html = withContext(Dispatchers.Default) {
            val js = when (readerExtractor) {
                ReaderExtractor.POSTLIGHT -> readRawResource(context, R.raw.mercury)
                ReaderExtractor.DEFUDDLE -> readRawResource(context, R.raw.defuddle)
            }
            """
            <html dir='auto'>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
              <script>$js</script>
            </head>
            <body />
            </html>
            """.trimIndent()
        }
    }

    class ReaderJSInterface {
        @JavascriptInterface
        fun onContentParsed(result: String) {
            contentLoaded(result)
        }
    }

    val webViewClient =
        remember(articleLink) {
            val linkUrl = articleLink.asJSString
            val content = articleContent.asJSString
            object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val parsingScript = when (readerExtractor) {
                        ReaderExtractor.POSTLIGHT -> {
                            //language=JavaScript
                            """
                                const link = $linkUrl;
                                const htmlContent = $content;
                                Mercury.parse(link, {html: htmlContent})
                                    .then(
                                        function(result) {
                                            let finalResult = JSON.stringify(result);
                                            window.ReaderJSInterface.onContentParsed(finalResult);
                                        }
                                    )
                            """.trimIndent()
                        }
                        ReaderExtractor.DEFUDDLE -> {
                            //language=JavaScript
                            """
                                const link = $linkUrl;
                                const htmlContent = $content;
                                
                                const parser = new DOMParser();
                                const doc = parser.parseFromString(htmlContent, 'text/html');
                                
                                const defuddle = new Defuddle(doc, {
                                     url: link
                                 });

                                const result = defuddle.parse();
                                
                                let finalResult = JSON.stringify(result);
                                window.ReaderJSInterface.onContentParsed(finalResult);
                            """.trimIndent()
                        }
                    }
                    view.evaluateJavascript(parsingScript, null)
                }
            }
        }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(Color.TRANSPARENT)
                addJavascriptInterface(ReaderJSInterface(), "ReaderJSInterface")
            }
        },
        modifier = modifier,
        update = { webView ->
            webView.webViewClient = webViewClient

            if (html.isNotBlank()) {
                webView.loadDataWithBaseURL(
                    /* baseUrl = */
                    articleLink,
                    /* data = */
                    html,
                    /* mimeType = */
                    "text/html",
                    /* encoding = */
                    "UTF-8",
                    /* historyUrl = */
                    null,
                )
            }
        },
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

private val String?.asJSString: String
    get() {
        val data = Json.encodeToString(this.orEmpty())
        return data
    }
