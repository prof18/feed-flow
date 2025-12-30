package com.prof18.feedflow.desktop.reaadermode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import co.touchlab.kermit.Logger
import com.prof18.feedflow.shared.domain.ReaderColors
import com.prof18.feedflow.shared.domain.getReaderModeStyledHtml
import com.prof18.feedflow.shared.domain.webview.WebViewNativeBridge
import java.io.File

@Composable
internal fun NativeWebViewContent(
    htmlContent: String,
    title: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
    logger: Logger? = null,
) {
    val bridge = remember { WebViewNativeBridge() }
    var isWebViewCreated by remember { mutableStateOf(false) }
    var lastBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    // Get colors from MaterialTheme
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb().toHexColor()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb().toHexColor()
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.toArgb().toHexColor()
    val borderColor = MaterialTheme.colorScheme.outline.toArgb().toHexColor()
    val bodyBackgroundColor = MaterialTheme.colorScheme.background.toArgb().toHexColor()

    val readerColors = remember(textColor, linkColor, backgroundColor, borderColor, bodyBackgroundColor) {
        ReaderColors(
            textColor = textColor,
            linkColor = linkColor,
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            bodyBackgroundColor = bodyBackgroundColor,
        )
    }

    // Create styled HTML with colors
    val styledHtml = remember(htmlContent, fontSize, readerColors, title) {
        getReaderModeStyledHtml(
            colors = readerColors,
            content = htmlContent,
            fontSize = fontSize,
            title = title,
        )
    }

    // Create the WebView when this composable enters the composition
    LaunchedEffect(Unit) {
        val result = bridge.createWebView()
        isWebViewCreated = result == 0
        if (result != 0) {
            logger?.e { "Failed to create WebView, result: $result" }
        }
    }

    // Load HTML content when it changes
    LaunchedEffect(styledHtml, isWebViewCreated) {
        if (!isWebViewCreated || styledHtml.isEmpty()) return@LaunchedEffect

        // Write HTML to a temp file
        val tempFile = File.createTempFile("feedflow_reader_", ".html")
        tempFile.writeText(styledHtml)
        tempFile.deleteOnExit()

        val result = bridge.loadHtmlFromFile(tempFile.absolutePath)
        if (result != 0) {
            logger?.e { "Failed to load HTML, result: $result" }
        }
    }

    // Cleanup when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            bridge.destroyWebView()
        }
    }

    // Track position and size of this composable
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                val bounds = layoutCoordinates.boundsInWindow()

                // Only update if bounds changed
                if (bounds != lastBounds) {
                    lastBounds = bounds

                    if (isWebViewCreated) {
                        // Convert to screen coordinates (density-independent)
                        val x = bounds.left.toDouble()
                        val y = bounds.top.toDouble()
                        val width = bounds.width.toDouble()
                        val height = bounds.height.toDouble()

                        if (width > 0 && height > 0) {
                            bridge.showWebView(x, y, width, height)
                        }
                    }
                }
            },
    )
}

private fun Int.toHexColor(): String {
    return String.format("#%06X", 0xFFFFFF and this)
}
