//
//  FallbackWebView.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import SwiftUI

struct FallbackWebView: View {
    var url: URL
    var onLinkClicked: ((URL) -> Void)?
    var topInset: CGFloat = 0

    @StateObject private var content = WebContent()
    @State private var isPageLoading = true
    @State private var loadedUrl: URL?

    var body: some View {
        ZStack {
            WebView(content: content, topInset: topInset)

            ReaderPlaceholder()
                .opacity(isPageLoading ? 1 : 0)
                .allowsHitTesting(isPageLoading)
                .animation(.default, value: isPageLoading)
        }
        .onAppear {
            setupLinkHandler()
            setupContentReadyHandler()
        }
        .onDisappear {
            content.detachHandlers()
        }
        .onAppearOrChange(url) { url in
            // Coming back from a pushed screen must keep the page and its scroll position. A load
            // that finished while hidden had its handlers detached, so that one loads again.
            if url == loadedUrl, !isPageLoading { return }
            loadedUrl = url
            isPageLoading = true
            setupContentReadyHandler()
            content.populate { content in
                content.load(url: url)
            }
        }
    }

    private func setupLinkHandler() {
        content.shouldBlockNavigation = { action -> Bool in
            if action.navigationType == .linkActivated, let url = action.request.url {
                DispatchQueue.main.async {
                    onLinkClicked?(url)
                }
                return true
            }
            return false
        }
    }

    private func setupContentReadyHandler() {
        content.onContentReady = {
            DispatchQueue.main.async {
                isPageLoading = false
            }
        }
    }
}
