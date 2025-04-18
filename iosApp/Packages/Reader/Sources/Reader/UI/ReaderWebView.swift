//
//  ReaderWebView.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import SwiftUI

struct ReaderWebView: View {
    var baseURL: URL
    var html: String
    var onLinkClicked: ((URL) -> Void)?

    @StateObject private var content = WebContent(transparent: true)

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
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
               url == .exitReaderModeLink || action.navigationType == .linkActivated {
                onLinkClicked?(url)
                return true
            }
            return false
        }
    }

    private struct Model: Equatable {
        var baseURL: URL
        var html: String
    }
}
