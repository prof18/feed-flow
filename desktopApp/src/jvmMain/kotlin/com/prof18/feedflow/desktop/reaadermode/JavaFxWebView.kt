package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import javax.swing.JPanel
import java.awt.BorderLayout

@Composable
internal fun JavaFxWebView(
    htmlContent: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {},
) {
    val jfxPanel = remember { JFXPanel() }
    val webViewHolder = remember { WebViewHolder() }

    DisposableEffect(Unit) {
        initializeJavaFx()
        onDispose {
            Platform.runLater {
                webViewHolder.webView?.engine?.load("about:blank")
            }
        }
    }

    LaunchedEffect(htmlContent) {
        Platform.runLater {
            if (webViewHolder.webView == null) {
                val webView = WebView()
                webViewHolder.webView = webView

                webView.engine.locationProperty().addListener { _, oldValue, newValue ->
                    if (newValue != null && newValue != oldValue && !newValue.isBlank() && newValue != "about:blank") {
                        Platform.runLater {
                            webView.engine.loadContent(htmlContent, "text/html")
                        }
                        onLinkClick(newValue)
                    }
                }

                val scene = Scene(webView)
                jfxPanel.scene = scene
            }

            webViewHolder.webView?.engine?.loadContent(htmlContent, "text/html")
        }
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            JPanel(BorderLayout()).apply {
                add(jfxPanel, BorderLayout.CENTER)
            }
        },
    )
}

private class WebViewHolder {
    var webView: WebView? = null
}

private var javaFxInitialized = false

private fun initializeJavaFx() {
    if (!javaFxInitialized) {
        javaFxInitialized = true
        Platform.setImplicitExit(false)
    }
}
