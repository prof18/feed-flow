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
    case extractedContent(html: String, baseURL: URL, url: URL)

    public func getUrl() -> URL? {
        switch self {
        case .fetching, .contentUnavailable:
            return nil
        case let .failedToExtractContent(url):
            return url
        case let .extractedContent(_, _, url):
            return url
        }
    }
}
