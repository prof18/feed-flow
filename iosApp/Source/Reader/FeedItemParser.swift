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

        let script = """
            (function() {
                console.log('Parsing content');
                const result = new Defuddle(
                    new DOMParser().parseFromString(\(request.htmlContent.asJSString), 'text/html'),
                    { url: \(request.url.asJSString) }
                ).parse();

                // Extract plain text from content
                let plainText = '';
                if (result.content) {
                    const tempDiv = document.createElement('div');
                    tempDiv.innerHTML = result.content;
                    plainText = (tempDiv.textContent || tempDiv.innerText || '').trim();
                }

                // Add plainText to result for validation
                result.plainText = plainText;

                return result;
            })();
        """

        webview.evaluateJavaScript(script) { [weak self] result, error in
            guard let self = self else { return }
            
            // "Claim" the current request to avoid race conditions with process termination
            guard var request = self.currentRequest else {
                return
            }
            self.currentRequest = nil
            
            // Re-usable finish block
            let finish = {
                self.isParsing = false
                self.processQueue()
            }

            if let error = error {
                print("JavaScript error:", error)

                // Attempt recovery if we haven't retried yet
                if request.retryCount < 1 {
                    print("Attempting to recover from WebView error/crash...")
                    request.retryCount += 1
                    
                    self.requestQueue.insert(request, at: 0)
                    self.isParsing = false
                    self.isReady = false
                    self.initializeWebView()
                    return
                } else {
                    // Already retried, fail the request
                    print("Recovery failed or max retries reached.")
                    request.onResult(ParsingResult.Error())
                    
                    self.isReady = false
                    self.initializeWebView()
                    finish() 
                    return
                }
            }

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
            return String(decoding: data, as: UTF8.self)
        } catch {
            return ""
        }
    }
}
