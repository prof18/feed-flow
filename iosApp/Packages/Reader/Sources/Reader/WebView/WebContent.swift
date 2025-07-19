import Combine
import Foundation
@preconcurrency import WebKit

class WebContent: NSObject, WKNavigationDelegate, WKUIDelegate, ObservableObject {
    fileprivate let webview: WKWebView
    private var observers = [NSKeyValueObservation]()
    private var subscriptions = Set<AnyCancellable>()
    private var populateBlock: ((WebContent) -> Void)?
    private var waitingForRepopulationAfterTermination = false

    var shouldBlockNavigation: ((WKNavigationAction) -> Bool)?
    var view: UIView { webview }
    var transparent: Bool = false {
        didSet(old) {
            if transparent != old { updateTransparency() }
        }
    }

    @Published private(set) var info = WebViewInfo()

    // swiftlint:disable:next function_body_length
    init(
        transparent: Bool = false,
        allowsInlinePlayback: Bool = false,
        autoplayAllowed: Bool = false
    ) {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = allowsInlinePlayback
        if autoplayAllowed {
            config.mediaTypesRequiringUserActionForPlayback = []
        }
        webview = WKWebView(frame: .zero, configuration: config)
        webview.allowsBackForwardNavigationGestures = true
        self.transparent = transparent
        super.init()
        webview.navigationDelegate = self
        webview.uiDelegate = self

        observers.append(
            webview.observe(
                \.url,
                changeHandler: { [weak self] _, _ in
                    self?.needsMetadataRefresh()
                }
            )
        )

        observers.append(
            webview.observe(
                \.url,
                changeHandler: { [weak self] _, _ in
                    self?.needsMetadataRefresh()
                }
            )
        )

        observers.append(
            webview.observe(
                \.canGoBack,
                changeHandler: { [weak self] _, val in
                    self?.info.canGoBack = val.newValue ?? false
                }
            )
        )

        observers.append(
            webview.observe(
                \.canGoForward,
                changeHandler: { [weak self] _, val in
                    self?.info.canGoForward = val.newValue ?? false
                }
            )
        )

        observers.append(
            webview.observe(
                \.isLoading,
                changeHandler: { [weak self] _, val in
                    self?.info.isLoading = val.newValue ?? false
                }
            )
        )

        webview.scrollView.backgroundColor = nil
        NotificationCenter.default.addObserver(
            self, selector: #selector(appDidForeground),
            name: UIApplication.willEnterForegroundNotification, object: nil
        )

        updateTransparency()
    }

    func load(url: URL) {
        webview.load(.init(url: url))
    }

    func load(html: String, baseURL: URL?) {
        webview.loadHTMLString(html, baseURL: baseURL)
    }

    private func updateTransparency() {
        webview.backgroundColor = transparent ? nil : UIColor.white
        webview.isOpaque = !transparent
    }

    func goBack() {
        webview.goBack()
    }

    func goForward() {
        webview.goForward()
    }

    func configure(_ block: (WKWebView) -> Void) {
        block(webview)
    }

    /// A webview's content process can be terminated while the app is in the background.
    /// `populate` allows you to handle this.
    /// Wrap your calls to load content into the webview within `populate`.
    /// The code will be called immediately, but _also_ after process termination.
    func populate(_ block: @escaping (WebContent) -> Void) {
        waitingForRepopulationAfterTermination = false
        populateBlock = block
        block(self)
    }

    @objc private func appDidForeground() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            if self.waitingForRepopulationAfterTermination, let block = self.populateBlock {
                block(self)
            }
            self.waitingForRepopulationAfterTermination = false
        }
    }

    func webView(_: WKWebView, didCommit _: WKNavigation!) {
        needsMetadataRefresh()
    }

    func webView(_: WKWebView, didFinish _: WKNavigation!) {
        needsMetadataRefresh()
    }

    func webView(
        _: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        if navigationAction.targetFrame?.isMainFrame ?? true,
           let block = shouldBlockNavigation,
           block(navigationAction) {
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    func webViewWebContentProcessDidTerminate(_: WKWebView) {
        waitingForRepopulationAfterTermination = true
    }

    func webView(
        _: WKWebView, createWebViewWith _: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction, windowFeatures _: WKWindowFeatures
    ) -> WKWebView? {
        // Load in same window:
        if let url = navigationAction.request.url {
            webview.load(.init(url: url))
        }
        return nil
    }

    private func needsMetadataRefresh() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) {
            self.refreshMetadataNow()
        }
    }

    private func refreshMetadataNow() {
        info = .init(
            url: webview.url, title: webview.title, canGoBack: webview.canGoBack,
            canGoForward: webview.canGoForward
        )
    }

    // MARK: - File Upload Prevention
    
    func webView(
        _ webView: WKWebView,
        runOpenPanelWith parameters: WKOpenPanelParameters,
        initiatedByFrame frame: WKFrameInfo,
        completionHandler: @escaping ([URL]?) -> Void
    ) {
        // Block all file upload attempts to prevent unwanted photo library access
        // RSS reader app should not need to upload files
        completionHandler(nil)
    }
}
