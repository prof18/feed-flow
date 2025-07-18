//
//  IosHtmlParser.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 01/04/23.
//  Copyright © 2023 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftSoup

class IosHtmlParser: HtmlParser {
    func getTextFromHTML(html: String) -> String? {
        guard !html.isEmpty else { return nil }

        let sanitizedHtml = sanitizeHtml(html)
        guard !sanitizedHtml.isEmpty else { return nil }

        do {
            let doc: Document = try SwiftSoup.parse(sanitizedHtml)
            return try doc.text()
        } catch {
            Deps.shared.getLogger(tag: "IosHtmlParser")
                .e(messageString: "Error during html parsing: \(error)")
            return nil
        }
    }

    func getFaviconUrl(html: String) -> String? {
        guard !html.isEmpty else { return nil }
        let sanitizedHtml = sanitizeHtml(html)

        do {
            let doc: Document = try SwiftSoup.parse(sanitizedHtml)

            let faviconLink = try doc.select("link[rel~=(?i)^(shortcut|icon)$][href]").first()
            return try faviconLink?.attr("href")
        } catch {
            Deps.shared.getLogger(tag: "IosHtmlParser")
                .e(messageString: "Error during getting the favicon: \(error)")
            return nil
        }
    }

    func getRssUrl(html: String) -> String? {
        guard !html.isEmpty else { return nil }
        let sanitizedHtml = sanitizeHtml(html)

        do {
            let doc: Document = try SwiftSoup.parse(sanitizedHtml)

            let queries = [
                "link[type='application/rss+xml']",
                "link[type='application/atom+xml']",
                "link[type='application/json']",
                "link[type='application/feed+json']"
            ]
            for query in queries {
                let element = try doc.select(query).first()
                let url = try element?.attr("href")
                if url != nil {
                    return url
                }
            }
            return nil
        } catch {
            Deps.shared.getLogger(tag: "IosHtmlParser")
                .e(messageString: "Error during getting the rss url: \(error)")
            return nil
        }
    }

    // Without this, SwiftSoup will still crash on some htmls
    private func sanitizeHtml(_ html: String) -> String {
        var sanitized = html

        // Remove null bytes and other problematic characters that can cause parser issues
        sanitized = sanitized.replacingOccurrences(of: "\0", with: "")
        sanitized = sanitized.replacingOccurrences(of: "\u{FEFF}", with: "") // BOM

        // Limit HTML size to prevent memory issues
        let maxHtmlSize = 1_000_000 // 1MB
        if sanitized.count > maxHtmlSize {
            sanitized = String(sanitized.prefix(maxHtmlSize))
        }

        // Fix truncated HTML that might cause parser issues
        sanitized = fixTruncatedHtml(sanitized)

        return sanitized
    }

    private func fixTruncatedHtml(_ html: String) -> String {
        var fixed = html

        // Remove incomplete tags at the end
        if let lastOpenBracket = fixed.lastIndex(of: "<") {
            let substring = fixed[lastOpenBracket...]
            if !substring.contains(">") {
                // Remove incomplete tag
                fixed = String(fixed[..<lastOpenBracket])
            }
        }

        // Fix incomplete attributes (especially problematic for srcset)
        // Pattern: attribute="incomplete_value_without_closing_quote
        let attributePattern = #"(\w+="[^"]*$)"#
        if let regex = try? NSRegularExpression(pattern: attributePattern) {
            let range = NSRange(fixed.startIndex ..< fixed.endIndex, in: fixed)
            fixed = regex.stringByReplacingMatches(in: fixed, options: [], range: range, withTemplate: "")
        }

        // Ensure the HTML ends cleanly - close any unclosed tags
        var openTags: [String] = []
        let tagPattern = #"<(/?)(\w+)[^>]*>"#
        if let regex = try? NSRegularExpression(pattern: tagPattern) {
            let range = NSRange(fixed.startIndex ..< fixed.endIndex, in: fixed)
            regex.enumerateMatches(in: fixed, options: [], range: range) { match, _, _ in
                guard let match = match else { return }

                if match.numberOfRanges >= 3 {
                    let isClosing = match.range(at: 1).length > 0
                    let tagRange = match.range(at: 2)
                    if let tagNameRange = Range(tagRange, in: fixed) {
                        let tagName = String(fixed[tagNameRange]).lowercased()

                        // Skip self-closing tags
                        if ["img", "br", "hr", "input", "meta", "link"].contains(tagName) {
                            return
                        }

                        if isClosing {
                            openTags.removeAll { $0 == tagName }
                        } else {
                            openTags.append(tagName)
                        }
                    }
                }
            }
        }

        // Close any remaining open tags
        for tag in openTags.reversed() {
            fixed += "</\(tag)>"
        }

        return fixed
    }
}
