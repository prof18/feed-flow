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
    @Binding var title: String?

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
            .onChange(of: content.info.title) { self.title = $0 }
    }

    private func setupLinkHandler() {
        content.shouldBlockNavigation = { action -> Bool in
            if action.navigationType == .linkActivated, let url = action.request.url {
                onLinkClicked?(url)
                return true
            }
            return false
        }
    }
}
