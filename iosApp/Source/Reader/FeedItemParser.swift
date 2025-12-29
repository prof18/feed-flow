//
//  FeedItemParser.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 11/04/25.
//

import FeedFlowKit
import Foundation
import WebKit

private let minContentLength = 200

class FeedItemParser: NSObject, WKUIDelegate, WKNavigationDelegate {
    static let shared = FeedItemParser()

    let webview: WKWebView

    private var requestQueue: [ParsingRequest] = []
    private var currentRequest: ParsingRequest?
    private var isParsing = false
    private var isReady = false

    override init() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = false
        config.mediaTypesRequiringUserActionForPlayback = .all
        // Enable shared HTTP cookie storage to access Safari cookies
        config.websiteDataStore = .default()

        self.webview = WKWebView(frame: .zero, configuration: config)
        
        super.init()
        webview.uiDelegate = self
        webview.navigationDelegate = self
        initializeWebView()
    }

    func parseArticle(url: String, htmlContent: String, onResult: @escaping (ParsingResult) -> Void) {
        let request = ParsingRequest(url: url, htmlContent: htmlContent, onResult: onResult)
        requestQueue.append(request)
        processQueue()
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

    private func processQueue() {
        guard isReady && !isParsing && !requestQueue.isEmpty else {
            return
        }

        isParsing = true
        currentRequest = requestQueue.removeFirst()

        guard let request = currentRequest else {
            isParsing = false
            return
        }

        let script = buildParsingScript(for: request)

        webview.evaluateJavaScript(script) { [weak self] result, error in
            guard let self = self else { return }
            self.handleJavaScriptResult(result: result, error: error)
        }
    }

    private func buildParsingScript(for request: ParsingRequest) -> String {
        """
        (function() {
            console.log('Parsing content');
            const result = new Defuddle(
                new DOMParser().parseFromString(\(request.htmlContent.asJSString), 'text/html'),
                { url: \(request.url.asJSString) }
            ).parse();

            // Convert relative URLs to absolute
            if (result.content) {
                const baseUrl = \(request.url.asJSString);
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = result.content;

                tempDiv.querySelectorAll('[src]').forEach(el => {
                    const src = el.getAttribute('src');
                    if (src && !src.startsWith('http') && !src.startsWith('data:')) {
                        try {
                            el.setAttribute('src', new URL(src, baseUrl).href);
                        } catch (e) {}
                    }
                });

                tempDiv.querySelectorAll('[href]').forEach(el => {
                    const href = el.getAttribute('href');
                    if (href && !href.startsWith('http') && !href.startsWith('#') && !href.startsWith('mailto:')) {
                        try {
                            el.setAttribute('href', new URL(href, baseUrl).href);
                        } catch (e) {}
                    }
                });

                result.content = tempDiv.innerHTML;
            }

            // Extract plain text from content
            let plainText = '';
            if (result.content) {
                const tempDiv2 = document.createElement('div');
                tempDiv2.innerHTML = result.content;
                plainText = (tempDiv2.textContent || tempDiv2.innerText || '').trim();
            }

            // Add plainText to result for validation
            result.plainText = plainText;

            return result;
        })();
        """
    }

    private func handleJavaScriptResult(result: Any?, error: Error?) {
        guard var request = currentRequest else {
            return
        }
        currentRequest = nil

        let finish = {
            self.isParsing = false
            self.processQueue()
        }

        if let error = error {
            handleJavaScriptError(error: error, request: &request, finish: finish)
            return
        }

        processParsingResult(result: result, request: request, finish: finish)
    }

    private func handleJavaScriptError(error: Error, request: inout ParsingRequest, finish: @escaping () -> Void) {
        print("JavaScript error:", error)

        if request.retryCount < 1 {
            print("Attempting to recover from WebView error/crash...")
            request.retryCount += 1

            self.requestQueue.insert(request, at: 0)
            self.isParsing = false
            self.isReady = false
            self.initializeWebView()
        } else {
            print("Recovery failed or max retries reached.")
            request.onResult(ParsingResult.Error())

            self.isReady = false
            self.initializeWebView()
            finish()
        }
    }

    private func processParsingResult(result: Any?, request: ParsingRequest, finish: @escaping () -> Void) {
        if let parsedJson = result as? [String: Any] {
            let title = parsedJson["title"] as? String
            let siteName = parsedJson["site"] as? String
            let htmlContent = parsedJson["content"] as? String
            let plainText = parsedJson["plainText"] as? String ?? ""

            if plainText.count < minContentLength {
                print("Content too short (\(plainText.count) chars), rejecting")
                Deps.shared.getLogger(tag: "FeedItemParser").w(
                    messageString: "Content too short (\(plainText.count) chars), rejecting: \(request.url)"
                )
                request.onResult(ParsingResult.Error())
                finish()
                return
            }

            let parsingResult = ParsingResult.Success(
                htmlContent: htmlContent,
                title: title,
                siteName: siteName
            )
            print("Parsed content: \(title ?? "")")
            request.onResult(parsingResult)
        } else {
            request.onResult(ParsingResult.Error())
        }

        finish()
    }

    func webView(_: WKWebView, didFinish _: WKNavigation!) {
        isReady = true
        processQueue()
    }
    
    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        print("WKWebView process terminated. Resetting...")
        
        // Recover in-flight request if present
        if var request = currentRequest {
            currentRequest = nil
            
            if request.retryCount < 1 {
                request.retryCount += 1
                requestQueue.insert(request, at: 0)
            } else {
                request.onResult(ParsingResult.Error())
            }
        }
        
        isReady = false
        isParsing = false
        initializeWebView()
    }
}

private struct ParsingRequest {
    let url: String
    let htmlContent: String
    let onResult: (ParsingResult) -> Void
    var retryCount: Int = 0
}

extension String {
    var asJSString: String {
        do {
            let data = try JSONSerialization.data(withJSONObject: self, options: .fragmentsAllowed)
            return String(bytes: data, encoding: .utf8) ?? ""
        } catch {
            return ""
        }
    }
}
