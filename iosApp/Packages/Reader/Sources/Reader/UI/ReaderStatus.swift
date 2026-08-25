//
//  ReaderStatus.swift
//  Reader
//
//  Created by Marco Gomiero on 18/04/25.
//

import Foundation

public enum ReaderStatus: Equatable {
    case fetching
    /// Nothing to show and no page to fall back to: the item carries no url of its own.
    case contentUnavailable
    case failedToExtractContent(url: URL)
    /// `contentId` identifies the document itself. It drives the web view reload, so it must
    /// change when the article or the content source changes, and must stay stable across
    /// styling-only rewrites of `html` (theme, font size, line height), which are applied to
    /// the loaded document with JS and would otherwise reset the scroll position.
    case extractedContent(html: String, baseURL: URL, url: URL, contentId: String)

    public func getUrl() -> URL? {
        switch self {
        case .fetching, .contentUnavailable:
            return nil
        case let .failedToExtractContent(url):
            return url
        case let .extractedContent(_, _, url, _):
            return url
        }
    }
}
