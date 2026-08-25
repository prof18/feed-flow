//
//  ReaderWebView.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import Foundation
import SwiftUI

struct ReaderWebView: View {
    var baseURL: URL
    var html: String
    var contentId: String
    var onLinkClicked: ((URL) -> Void)?
    var onImageClicked: ((URL) -> Void)?
    var onWebContentReady: ((WebContent) -> Void)?

    @StateObject private var content = WebContent(transparent: true, allowsZooming: false)

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
                onWebContentReady?(content)
            }
            .onAppearOrChange(Model(baseURL: baseURL, html: html, contentId: contentId)) { model in
                content.populate { content in
                    content.load(html: model.html, baseURL: model.baseURL)
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
