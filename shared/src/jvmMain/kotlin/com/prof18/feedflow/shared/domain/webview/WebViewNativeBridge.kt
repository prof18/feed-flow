package com.prof18.feedflow.shared.domain.webview

class WebViewNativeBridge {
    /**
     * Creates a WebView window
     * Returns: 0 = success, 1 = error
     */
    external fun createWebView(): Int

    /**
     * Loads HTML content from a file path
     * Returns: 0 = success, 1 = webview not created, 2 = file read error
     */
    external fun loadHtmlFromFile(filePath: String): Int

    /**
     * Shows the WebView at the specified position and size
     * Returns: 0 = success, 1 = webview not created, 2 = main window not found
     */
    external fun showWebView(x: Double, y: Double, width: Double, height: Double): Int

    /**
     * Hides the WebView
     * Returns: 0 = success, 1 = webview not created
     */
    external fun hideWebView(): Int

    /**
     * Destroys the WebView and releases resources
     * Returns: 0 = success
     */
    external fun destroyWebView(): Int

    /**
     * Updates the WebView position (called when window moves/resizes)
     * Returns: 0 = success, 1 = webview not created, 2 = main window not found
     */
    external fun updatePosition(x: Double, y: Double, width: Double, height: Double): Int
}

@Suppress("MagicNumber")
enum class WebViewResult(val code: Int) {
    SUCCESS(0),
    WEBVIEW_NOT_CREATED(1),
    FILE_READ_ERROR(2),
    MAIN_WINDOW_NOT_FOUND(2),
    UNKNOWN_ERROR(-1),
    ;

    companion object {
        fun fromCode(code: Int): WebViewResult {
            return entries.find { it.code == code } ?: UNKNOWN_ERROR
        }
    }
}
