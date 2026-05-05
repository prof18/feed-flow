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

    @StateObject private var content = WebContent()
    @State private var isPageLoading = true

    var body: some View {
        ZStack {
            WebView(content: content)

            ReaderPlaceholder()
                .opacity(isPageLoading ? 1 : 0)
                .allowsHitTesting(isPageLoading)
                .animation(.default, value: isPageLoading)
        }
        .onAppear {
            setupLinkHandler()
            setupContentReadyHandler()
        }
        .onAppearOrChange(url) { url in
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
