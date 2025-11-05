//
//  FeedItemParser.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 11/04/25.
//

import Foundation
import FeedFlowKit
import WebKit

class FeedItemParser: NSObject, WKUIDelegate, WKNavigationDelegate {
    static let shared = FeedItemParser()

    let webview = WKWebView()

    private var url: String?
    private var htmlContent: String?

    private var onResult: ((ParsingResult) -> Void)?

    override init() {
        super.init()
        webview.uiDelegate = self
        webview.navigationDelegate = self
    }

    func parseArticle(url: String, htmlContent: String, onResult: @escaping (ParsingResult) -> Void) {
        self.url = url
        self.htmlContent = htmlContent
        self.onResult = onResult
        initializeWebView()
    }

    private func initializeWebView() {
        var bundleIdentifier: String
        #if DEBUG
            bundleIdentifier = "com.prof18.feedflow.dev"
        #else
            bundleIdentifier = "com.prof18.feedflow"
        #endif

        let bundleURL = Bundle(identifier: bundleIdentifier)?.url(forResource: "defuddle", withExtension: "js")
        let fallbackURL = Bundle.main.url(forResource: "defuddle", withExtension: "js")

        guard let resourceURL = bundleURL ?? fallbackURL else {
            return
        }

        let defuddleJS = try? String(contentsOf: resourceURL)

        if let defuddleJS = defuddleJS {
            let html = """
             <body>
                 <script>\(defuddleJS)</script>
             </body>
            """
            webview.loadHTMLString(html, baseURL: nil)
        }
    }

    func webView(_: WKWebView, didFinish _: WKNavigation!) {
        if let htmlContent = htmlContent, let url = url {
            let script = """
                console.log('Parsing content');
                new Defuddle(
                    new DOMParser().parseFromString(\(htmlContent.asJSString), 'text/html'),
                    { url: \(url.asJSString) }
                ).parse();
            """

            webview.evaluateJavaScript(script) { result, error in
                if let error = error {
                    print("JavaScript error:", error)
                    self.onResult?(ParsingResult.Error())
                    return
                }

                if let parsedJson = result as? [String: Any] {
                    let title = parsedJson["title"] as? String
                    let siteName = parsedJson["site"] as? String
                    let parsingResult = ParsingResult.Success(
                        htmlContent: parsedJson["content"] as? String,
                        title: title,
                        siteName: siteName
                    )
                    print("Parsed content: \(title ?? "")")
                    self.onResult?(parsingResult)
                } else {
                    self.onResult?(ParsingResult.Error())
                }
            }
        }
    }
}

extension String {
    var asJSString: String {
        do {
            let data = try JSONSerialization.data(withJSONObject: self, options: .fragmentsAllowed)
            return String(decoding: data, as: UTF8.self)
        } catch {
            return ""
        }
    }
}
