//
//  HtmlView.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 22/07/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import WebKit
import SwiftUI

struct HTMLStringView: UIViewRepresentable {
    let htmlContent: String

    func makeUIView(context: Context) -> WKWebView {
        return WKWebView()
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        uiView.loadHTMLString(htmlContent, baseURL: nil)
    }
}
