import Foundation
import WebKit

class MercuryExtractor: NSObject, WKUIDelegate, WKNavigationDelegate {
    static let shared = MercuryExtractor()

    let webview = WKWebView()

    override init() {
        super.init()
        webview.uiDelegate = self
        webview.navigationDelegate = self
    }

    private func initializeJS() {
        guard readyState == .none else { return }
        Reader.logger.info("Initializing...")
        readyState = .initializing
        let mercuryJS = try! String(contentsOf: Bundle.module.url(forResource: "mercury.web", withExtension: "js")!)
        let html = """
<body>
    <script>\(mercuryJS)</script>
    <script>alert('ok')</script>
</body>
"""
        webview.loadHTMLString(html, baseURL: nil)
    }

    func warmUp() {
        // do nothing -- the real warmup is done in init
        initializeJS()
    }

    typealias ReadyBlock = () -> Void
    private var pendingReadyBlocks = [ReadyBlock]()

    private enum ReadyState {
        case none
        case initializing
        case ready
    }

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

    private func waitUntilReady(_ callback: @escaping ReadyBlock) {
        switch readyState {
        case .ready: callback()
        case .none:
            pendingReadyBlocks.append(callback)
            initializeJS()
        case .initializing:
            pendingReadyBlocks.append(callback)
        }
    }

    typealias Callback = (ExtractedContent?) -> Void

    // TODO: queue up simultaneous requests?
    func extract(html: String, url: URL, callback: @escaping Callback) {
        waitUntilReady {
            let script = "return await Mercury.parse(\(url.absoluteString.asJSString), {html: \(html.asJSString)})"

            self.webview.callAsyncJavaScript(script, arguments: [:], in: nil, in: .page) { result in
                switch result {
                case .failure(let err):
                    Reader.logger.error("Failed to extract: \(err)")
                    callback(nil)
                case .success(let resultOpt):
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

    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String, initiatedByFrame frame: WKFrameInfo) async {
        if message == "ok" {
            DispatchQueue.main.async {
                self.readyState = .ready
                Reader.logger.info("Ready")
            }
        }
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        Reader.logger.info("Web process did terminate")
        self.readyState = .none
    }

    private func parse(dict: [String: Any]?) -> ExtractedContent? {
        guard let result = dict else { return nil }
        let content = ExtractedContent(
            content: result["content"] as? String,
            author: result["author"] as? String,
            title: result["title"] as? String,
            excerpt: result["excerpt"] as? String
        )
        return content
    }
}
