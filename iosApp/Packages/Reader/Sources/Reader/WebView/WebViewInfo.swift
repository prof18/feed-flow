//
//  WebViewInfo.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//
import Foundation

struct WebViewInfo: Equatable, Codable {
    var url: URL?
    var title: String?
    var canGoBack = false
    var canGoForward = false
    var isLoading = false
}
