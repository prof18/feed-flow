package com.prof18.webview

import com.prof18.jni.JNIEnvVar
import com.prof18.jni.jclass
import com.prof18.jni.jdouble
import com.prof18.jni.jstring
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.AppKit.NSApp
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSColor
import platform.AppKit.NSWindow
import platform.AppKit.NSWindowAbove
import platform.AppKit.NSWindowStyleMaskBorderless
import platform.Foundation.NSMakeRect
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

private var webViewWindow: NSWindow? = null
private var webView: WKWebView? = null
private var navigationDelegate: WebViewNavigationDelegate? = null

/**
 * Creates a WebView window
 * Returns: 0 = success, 1 = error
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_createWebView")
fun createWebView(env: CPointer<JNIEnvVar>, clazz: jclass): Int {
    if (webViewWindow != null) {
        return 0 // Already created
    }

    val configuration = WKWebViewConfiguration()

    val wkWebView = WKWebView(
        frame = NSMakeRect(0.0, 0.0, 800.0, 600.0),
        configuration = configuration
    )

    navigationDelegate = WebViewNavigationDelegate()
    wkWebView.navigationDelegate = navigationDelegate
    wkWebView.allowsBackForwardNavigationGestures = false

    val window = NSWindow(
        contentRect = NSMakeRect(0.0, 0.0, 800.0, 600.0),
        styleMask = NSWindowStyleMaskBorderless,
        backing = NSBackingStoreBuffered,
        defer = false
    )

    window.isOpaque = false
    window.backgroundColor = NSColor.clearColor
    window.contentView = wkWebView
    window.level = 0
    window.hasShadow = false

    webViewWindow = window
    webView = wkWebView

    return 0
}

/**
 * Loads HTML content from a file path
 * The Java side writes HTML to a temp file and passes the path
 * Returns: 0 = success, 1 = webview not created, 2 = file read error
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_loadHtmlFromFile")
fun loadHtmlFromFile(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    filePathJString: jstring?,
): Int {
    val wkWebView = webView ?: return 1
    val filePath = filePathJString?.toKotlinString(env) ?: return 2

    val htmlContent = NSString.stringWithContentsOfFile(
        filePath,
        encoding = NSUTF8StringEncoding,
        error = null
    ) ?: return 2

    wkWebView.loadHTMLString(htmlContent, baseURL = null)

    return 0
}

/**
 * Shows the WebView at the specified position and size
 * Returns: 0 = success, 1 = webview not created, 2 = main window not found
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_showWebView")
fun showWebView(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    x: jdouble,
    y: jdouble,
    width: jdouble,
    height: jdouble
): Int {
    val window = webViewWindow ?: return 1
    val wkWebView = webView ?: return 1

    val mainWindow = NSApp?.mainWindow ?: NSApp?.windows?.firstOrNull() as? NSWindow ?: return 2

    val mainFrame = mainWindow.frame.useContents {
        NSMakeRect(origin.x, origin.y, size.width, size.height)
    }

    val mainWindowX = mainFrame.useContents { origin.x }
    val mainWindowY = mainFrame.useContents { origin.y }
    val mainWindowHeight = mainFrame.useContents { size.height }

    // Convert from top-left (Java/Compose) to bottom-left (macOS) coordinate system
    val webViewX = mainWindowX + x
    val webViewY = mainWindowY + mainWindowHeight - y - height

    window.setFrame(NSMakeRect(webViewX, webViewY, width, height), display = true)
    wkWebView.setFrame(NSMakeRect(0.0, 0.0, width, height))

    // Make the window a child of the main window so it moves with it
    mainWindow.addChildWindow(window, ordered = NSWindowAbove)
    window.orderFront(null)

    return 0
}

/**
 * Hides the WebView
 * Returns: 0 = success, 1 = webview not created
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_hideWebView")
fun hideWebView(env: CPointer<JNIEnvVar>, clazz: jclass): Int {
    val window = webViewWindow ?: return 1

    window.parent?.removeChildWindow(window)
    window.orderOut(null)

    return 0
}

/**
 * Destroys the WebView and releases resources
 * Returns: 0 = success
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_destroyWebView")
fun destroyWebView(env: CPointer<JNIEnvVar>, clazz: jclass): Int {
    val window = webViewWindow

    if (window != null) {
        window.parent?.removeChildWindow(window)
        window.orderOut(null)
        window.close()
    }

    webView?.navigationDelegate = null
    webView = null
    webViewWindow = null
    navigationDelegate = null
    currentHtmlFilePath = null

    return 0
}

/**
 * Updates the WebView position (called when window moves/resizes)
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Suppress("UNUSED_PARAMETER")
@CName("Java_com_prof18_feedflow_shared_domain_webview_WebViewNativeBridge_updatePosition")
fun updatePosition(
    env: CPointer<JNIEnvVar>,
    clazz: jclass,
    x: jdouble,
    y: jdouble,
    width: jdouble,
    height: jdouble
): Int {
    val window = webViewWindow ?: return 1
    val wkWebView = webView ?: return 1

    val mainWindow = NSApp?.mainWindow ?: NSApp?.windows?.firstOrNull() as? NSWindow ?: return 2

    val mainFrame = mainWindow.frame.useContents {
        NSMakeRect(origin.x, origin.y, size.width, size.height)
    }

    val mainWindowX = mainFrame.useContents { origin.x }
    val mainWindowY = mainFrame.useContents { origin.y }
    val mainWindowHeight = mainFrame.useContents { size.height }

    val webViewX = mainWindowX + x
    val webViewY = mainWindowY + mainWindowHeight - y - height

    window.setFrame(NSMakeRect(webViewX, webViewY, width, height), display = true)
    wkWebView.setFrame(NSMakeRect(0.0, 0.0, width, height))

    return 0
}

// Navigation delegate to handle link clicks
@OptIn(ExperimentalForeignApi::class)
private class WebViewNavigationDelegate : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val request = decidePolicyForNavigationAction.request
        val url = request.URL?.absoluteString

        // Allow loading of the initial HTML content (no URL or about:blank)
        if (url == null || url.isEmpty() || url == "about:blank") {
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
            return
        }

        // For clicked links, open in default browser and cancel navigation
        if (decidePolicyForNavigationAction.navigationType.toInt() == 0) { // LinkActivated
            request.URL?.let { nsUrl ->
                platform.AppKit.NSWorkspace.sharedWorkspace.openURL(nsUrl)
            }
            decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
            return
        }

        // Allow other navigation types (like initial page load)
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    }
}
