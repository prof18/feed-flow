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

    var body: some View {
        WebView(content: content)
            .onAppear {
                setupLinkHandler()
            }
            .onAppearOrChange(url) { url in
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
}
