import Foundation
import Fuzi
import SwiftSoup

struct SiteMetadata: Equatable, Codable {
    public var url: URL
    public var title: String?
    public var description: String?
    public var heroImage: URL?
    public var favicon: URL?

    private struct MetadataParseError: Error {}

    public static func extractMetadata(fromHTML html: String, baseURL: URL) async throws
        -> SiteMetadata {
        try await withCheckedThrowingContinuation { continuation in
            DispatchQueue.metadataExtractorQueue.async {
                do {
                    let doc = try HTMLDocument(stringSAFE: html)

                    let metadata = SiteMetadata(
                        url: baseURL,
                        title: doc.ogTitle ?? doc.title,
                        description: doc.metaDescription?.nilIfEmpty,
                        heroImage: doc.ogImage(baseURL: baseURL),
                        favicon: doc.favicon(baseURL: baseURL) ?? baseURL.inferredFaviconURL
                    )
                    continuation.resume(returning: metadata)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }
}

private extension DispatchQueue {
    static let metadataExtractorQueue = DispatchQueue(
        label: "MetadataExtractor",
        qos: .default,
        attributes: .concurrent
    )
}

private extension Fuzi.HTMLDocument {
    private func getAttribute(selector: String, attribute: String) -> String? {
        return css(selector).first?.attr(attribute, namespace: nil)
    }

    var metaDescription: String? {
        getAttribute(selector: "meta[name='description']", attribute: "content")
    }

    var ogTitle: String? {
        getAttribute(selector: "meta[property='og:title']", attribute: "content")
    }

    func ogImage(baseURL: URL) -> URL? {
        if let link = getAttribute(selector: "meta[property='og:image']", attribute: "content") {
            return URL(string: link, relativeTo: baseURL)
        }
        return nil
    }

    func favicon(baseURL: URL) -> URL? {
        for item in css("link") {
            if let rel = item.attr("rel", namespace: nil),
               rel == "icon" || rel == "shortcut icon",
               let val = item.attr("href", namespace: nil),
               let resolved = URL(string: val, relativeTo: baseURL) {
                return resolved
            }
        }
        return nil
    }
}
