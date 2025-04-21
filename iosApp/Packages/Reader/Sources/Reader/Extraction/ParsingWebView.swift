//
//  ParsingWebView.swift
//  Reader
//
//  Created by Marco Gomiero on 21/04/25.
//

import Foundation
import WebKit

class ParsingWebView: NSObject, WKUIDelegate, WKNavigationDelegate {
    static let shared = ParsingWebView()
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
            let script = """
                new Defuddle(
                    new DOMParser().parseFromString(\(html.asJSString), 'text/html'),
                    { url: \(url.absoluteString.asJSString) }
                ).parse();
            """

            self.webview.evaluateJavaScript(script) { result, error in
                if let error = error {
                    Reader.logger.error("Failed to extract: \(error)")
                    callback(nil)
                    return
                }
                Reader.logger.info("Successfully extracted")
                let content = self.parse(dict: result as? [String: Any])
                if let content, content.plainText.count >= 200 {
                    callback(content)
                } else {
                    callback(nil)
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
        let defuddleJS = try? String(
            contentsOf: Bundle.module.url(forResource: "defuddle", withExtension: "js")!
        )
        let html = """
        <body>
            <script>\(defuddleJS ?? "")</script>
            <script>alert('ok')</script>
        </body>
        """
        webview.loadHTMLString(html, baseURL: nil)
    }

    private func parse(dict: [String: Any]?) -> ExtractedContent? {
        guard let result = dict else { return nil }
        let content = ExtractedContent(
            content: result["content"] as? String,
            title: result["title"] as? String
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
