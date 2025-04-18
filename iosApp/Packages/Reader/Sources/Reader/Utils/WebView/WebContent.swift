import Combine
import Foundation
import WebKit

class WebContent: NSObject, WKNavigationDelegate, WKUIDelegate, ObservableObject {
    fileprivate let webview: WKWebView
    private var observers = [NSKeyValueObservation]()
    private var subscriptions = Set<AnyCancellable>()

    // MARK: - API

    struct Info: Equatable, Codable {
        var url: URL?
        var title: String?
        var canGoBack = false
        var canGoForward = false
        var isLoading = false
    }

    @Published private(set) var info = Info()
    var shouldBlockNavigation: ((WKNavigationAction) -> Bool)?

    func load(url: URL) {
        webview.load(.init(url: url))
    }

    func load(html: String, baseURL: URL?) {
        webview.loadHTMLString(html, baseURL: baseURL)
    }

    init(transparent: Bool = false, allowsInlinePlayback: Bool = false, autoplayAllowed: Bool = false) {
        let config = WKWebViewConfiguration()
        #if os(iOS)
            config.allowsInlineMediaPlayback = allowsInlinePlayback
            if autoplayAllowed {
                config.mediaTypesRequiringUserActionForPlayback = []
            }
        #endif
        webview = WKWebView(frame: .zero, configuration: config)
        webview.allowsBackForwardNavigationGestures = true
        self.transparent = transparent
        super.init()
        webview.navigationDelegate = self
        webview.uiDelegate = self

        observers.append(webview.observe(\.url, changeHandler: { [weak self] _, _ in
            self?.needsMetadataRefresh()
        }))

        observers.append(webview.observe(\.url, changeHandler: { [weak self] _, _ in
            self?.needsMetadataRefresh()
        }))

        observers.append(webview.observe(\.canGoBack, changeHandler: { [weak self] _, val in
            self?.info.canGoBack = val.newValue ?? false
        }))

        observers.append(webview.observe(\.canGoForward, changeHandler: { [weak self] _, val in
            self?.info.canGoForward = val.newValue ?? false
        }))

        observers.append(webview.observe(\.isLoading, changeHandler: { [weak self] _, val in
            self?.info.isLoading = val.newValue ?? false
        }))

        #if os(macOS)
        // no op
        #else
            webview.scrollView.backgroundColor = nil
            NotificationCenter.default.addObserver(self, selector: #selector(appDidForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
        #endif
        updateTransparency()
    }

    var transparent: Bool = false {
        didSet(old) {
            if transparent != old { updateTransparency() }
        }
    }

    private func updateTransparency() {
        #if os(macOS)
        // TODO: Implement transparency on macOS
        #else
            webview.backgroundColor = transparent ? nil : UINSColor.white
            webview.isOpaque = !transparent
        #endif
    }

    #if os(macOS)
        var view: NSView { webview }
    #else
        var view: UIView { webview }
    #endif

    func goBack() {
        webview.goBack()
    }

    func goForward() {
        webview.goForward()
    }

    func configure(_ block: (WKWebView) -> Void) {
        block(webview)
    }

    // MARK: - Populate

    private var populateBlock: ((WebContent) -> Void)?
    private var waitingForRepopulationAfterProcessTerminate = false
    /// A webview's content process can be terminated while the app is in the background.
    /// `populate` allows you to handle this.
    /// Wrap your calls to load content into the webview within `populate`.
    /// The code will be called immediately, but _also_ after process termination.
    func populate(_ block: @escaping (WebContent) -> Void) {
        waitingForRepopulationAfterProcessTerminate = false
        populateBlock = block
        block(self)
    }

    // MARK: - Lifecycle

    @objc private func appDidForeground() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            if self.waitingForRepopulationAfterProcessTerminate, let block = self.populateBlock {
                block(self)
            }
            self.waitingForRepopulationAfterProcessTerminate = false
        }
    }

    // MARK: - WKNavigationDelegate

    func webView(_: WKWebView, didCommit _: WKNavigation!) {
        needsMetadataRefresh()
    }

    func webView(_: WKWebView, didFinish _: WKNavigation!) {
        needsMetadataRefresh()
    }

    func webView(_: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if navigationAction.targetFrame?.isMainFrame ?? true,
           let block = shouldBlockNavigation,
           block(navigationAction) {
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    func webViewWebContentProcessDidTerminate(_: WKWebView) {
        waitingForRepopulationAfterProcessTerminate = true
    }

    // MARK: - WKUIDelegate

    func webView(_: WKWebView, createWebViewWith _: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures _: WKWindowFeatures) -> WKWebView? {
        // Load in same window:
        if let url = navigationAction.request.url {
            webview.load(.init(url: url))
        }
        return nil
    }

    // MARK: - Metadata

    private func needsMetadataRefresh() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.01) {
            self.refreshMetadataNow()
        }
    }

    private func refreshMetadataNow() {
        info = .init(url: webview.url, title: webview.title, canGoBack: webview.canGoBack, canGoForward: webview.canGoForward)
    }
}
