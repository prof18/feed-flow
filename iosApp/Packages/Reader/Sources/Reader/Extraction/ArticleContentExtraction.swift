import Foundation
import WebKit
import Fuzi

public enum ExtractionError: Error {
    case DataIsNotString
    case FailedToExtract
    case MissingExtractionData
}

public struct ExtractedContent: Equatable {
    // See https://github.com/postlight/mercury-parser#usage
    public var content: String?
    public var author: String?
    public var title: String?
    public var excerpt: String?
    public var date_published: String?
}

extension ExtractedContent {
    public var datePublished: Date? {
        date_published.flatMap { Self.dateParser.date(from: $0) }
    }
    static let dateParser = ISO8601DateFormatter()

    var plainText: String {
        if let content {
            let parsed = try? HTMLDocument(data: content.data(using: .utf8)!)
            return parsed?.root?.stringValue.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        return ""
    }
}

public enum Extractor: Equatable {
    case mercury
}
