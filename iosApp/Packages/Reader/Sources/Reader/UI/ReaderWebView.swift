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
    var onLinkClicked: ((URL) -> Void)?
    var onImageClicked: ((URL) -> Void)?
    var onWebContentReady: ((WebContent) -> Void)?

    @StateObject private var content = WebContent(transparent: true)

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
                onWebContentReady?(content)
            }
            .onAppearOrChange(Model(baseURL: baseURL, html: html)) { model in
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

        // Compare only by URL to prevent unnecessary reloads when the same content
        // is re-emitted (e.g., when app goes to background), which would reset scroll position
        static func == (lhs: Model, rhs: Model) -> Bool {
            lhs.baseURL == rhs.baseURL
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

    return isHttpUrl && !isLocalhost
}
