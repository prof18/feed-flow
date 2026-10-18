//
//  ReaderWebView.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import Foundation
import SwiftUI

struct ReaderWebView: View {
    /// `nil` while the first article is still being fetched: the web view is created anyway so its
    /// WebKit process starts alongside extraction instead of after it.
    var article: DisplayedArticle?
    /// False for a moment after Next/Previous, so a quick switch goes straight to the next article.
    var allowsPlaceholder = true
    var onLinkClicked: ((URL) -> Void)?
    var onImageClicked: ((URL) -> Void)?
    var topInset: CGFloat = 0
    var onWebContentReady: ((WebContent) -> Void)?

    @StateObject private var content = WebContent(transparent: true, allowsZooming: false)
    @State private var loadingContentId: String?
    @State private var paintedContentId: String?

    /// Derived from the current document rather than set from a load callback, so when the web view
    /// is handed the next article the placeholder covers the previous one in that same frame.
    private var showsPlaceholder: Bool {
        allowsPlaceholder && (article == nil || paintedContentId != article?.contentId)
    }

    var body: some View {
        ZStack {
            WebView(content: content, topInset: topInset)

            // Keeps the article placeholder up until WebKit has painted the page, instead of a blank surface.
            ReaderPlaceholder()
                .padding(.top, topInset)
                .opacity(showsPlaceholder ? 1 : 0)
                .allowsHitTesting(showsPlaceholder)
                .animation(showsPlaceholder ? nil : .default, value: showsPlaceholder)
        }
        .onAppear {
            setupLinkHandler()
            setupLoadingHandler()
            onWebContentReady?(content)
        }
        .onDisappear {
            content.detachHandlers()
        }
        .onAppearOrChange(article.map { Model(baseURL: $0.baseURL, html: $0.html, contentId: $0.contentId) }) { model in
            guard let model else { return }
            loadingContentId = model.contentId
            content.populate { content in
                content.load(html: model.html, baseURL: model.baseURL)
            }
        }
    }

    private func setupLoadingHandler() {
        content.onLoadingStateChanged = { [weak content] isLoading in
            guard !isLoading else { return }
            DispatchQueue.main.async {
                content?.afterNextPaint {
                    paintedContentId = loadingContentId
                }
            }
        }
    }

    private func setupLinkHandler() {
        content.shouldBlockNavigation = { action -> Bool in
            if let url = action.request.url,
               url.scheme == "feedflow-image" {
                if let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                   let src = components.queryItems?.first(where: { $0.name == "src" })?.value,
                   let imageUrl = URL(string: src),
                   isValidImageUrl(imageUrl) {
                    DispatchQueue.main.async {
                        onImageClicked?(imageUrl)
                    }
                }
                return true
            }

            if let url = action.request.url,
               action.navigationType == .linkActivated {
                DispatchQueue.main.async {
                    onLinkClicked?(url)
                }
                return true
            }
            return false
        }
    }

    private struct Model: Equatable {
        var baseURL: URL
        var html: String
        var contentId: String

        // A change here reloads the web view and sends the reader back to the top, so the
        // document identity is what counts. `html` is deliberately left out: theme, font size
        // and line height rewrite it while pointing at the same document, and they are applied
        // to the live page with JS instead.
        static func == (lhs: Model, rhs: Model) -> Bool {
            lhs.baseURL == rhs.baseURL && lhs.contentId == rhs.contentId
        }
    }
}

private func isValidImageUrl(_ url: URL) -> Bool {
    let scheme = url.scheme?.lowercased()
    let isHttpUrl = scheme == "http" || scheme == "https"

    let host = url.host?.lowercased() ?? ""
    let isLocalhost = host.contains("localhost") ||
                     host.contains("127.0.0.1") ||
                     host.contains("0.0.0.0") ||
                     host.contains("::1")

    let pathExtension = url.pathExtension.lowercased()
    let isUnsupportedFullscreenFormat = ["pdf", "svg", "svgz"].contains(pathExtension)

    return isHttpUrl && !isLocalhost && !isUnsupportedFullscreenFormat
}
