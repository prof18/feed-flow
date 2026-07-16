//
//  HtmlView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import SwiftUI
import WebKit

struct HTMLStringView: UIViewRepresentable {
    let htmlContent: String

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context _: Context) -> WKWebView {
        return WKWebView()
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        guard context.coordinator.loadedHTML != htmlContent else { return }
        context.coordinator.loadedHTML = htmlContent
        uiView.loadHTMLString(htmlContent, baseURL: nil)
    }

    final class Coordinator {
        var loadedHTML: String?
    }
}
