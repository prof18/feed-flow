import Foundation
import WebKit

class MercuryExtractor: NSObject, WKUIDelegate, WKNavigationDelegate {
    static let shared = MercuryExtractor()
    let webview = WKWebView()

    private var pendingReadyBlocks = [() -> Void]()

    private var readyState = ReadyState.none {
        didSet {
            if readyState == .ready {
                for block in pendingReadyBlocks {
                    block()
                }
                pendingReadyBlocks.removeAll()
            }
        }
    }

    override init() {
        super.init()
        webview.uiDelegate = self
        webview.navigationDelegate = self
    }

    func warmUp() {
        initializeJS()
    }

    func extract(html: String, url: URL, callback: @escaping (ExtractedContent?) -> Void) {
        waitUntilReady {
            let script =
                "return await Mercury.parse(\(url.absoluteString.asJSString), {html: \(html.asJSString)})"

            self.webview.callAsyncJavaScript(script, arguments: [:], in: nil, in: .page) { result in
                switch result {
                case let .failure(err):
                    Reader.logger.error("Failed to extract: \(err)")
                    callback(nil)
                case let .success(resultOpt):
                    Reader.logger.info("Successfully extracted")
                    let content = self.parse(dict: resultOpt as? [String: Any])
                    if let content, content.plainText.count >= 200 {
                        callback(content)
                    } else {
                        callback(nil)
                    }
                }
            }
        }
    }

    func webView(
        _: WKWebView,
        runJavaScriptAlertPanelWithMessage message: String,
        initiatedByFrame _: WKFrameInfo
    ) async {
        if message == "ok" {
            DispatchQueue.main.async {
                self.readyState = .ready
                Reader.logger.info("Ready")
            }
        }
    }

    func webViewWebContentProcessDidTerminate(_: WKWebView) {
        Reader.logger.info("Web process did terminate")
        readyState = .none
    }

    private func initializeJS() {
        guard readyState == .none else { return }
        Reader.logger.info("Initializing...")
        readyState = .initializing
        let mercuryJS = try? String(
            contentsOf: Bundle.module.url(forResource: "mercury.web", withExtension: "js")!
        )
        let html = """
        <body>
            <script>\(mercuryJS ?? "")</script>
            <script>alert('ok')</script>
        </body>
        """
        webview.loadHTMLString(html, baseURL: nil)
    }

    private func parse(dict: [String: Any]?) -> ExtractedContent? {
        guard let result = dict else { return nil }
        let content = ExtractedContent(
            content: result["content"] as? String,
            author: result["author"] as? String,
            title: result["title"] as? String,
            excerpt: result["excerpt"] as? String,
            direction: result["direction"] as? String
        )
        return content
    }

    private func waitUntilReady(_ callback: @escaping () -> Void) {
        switch readyState {
        case .ready: callback()
        case .none:
            pendingReadyBlocks.append(callback)
            initializeJS()
        case .initializing:
            pendingReadyBlocks.append(callback)
        }
    }
}

private enum ReadyState {
    case none
    case initializing
    case ready
}
