import Foundation
import Fuzi
import WebKit

enum ExtractionError: Error {
    case dataIsNotString
    case failedToExtract
    case missingExtractionData
}

struct ExtractedContent: Equatable {
    // See https://github.com/postlight/mercury-parser#usage
    public var content: String?
    public var author: String?
    public var title: String?
    public var excerpt: String?
    public var direction: String?
}

extension ExtractedContent {
    var plainText: String {
        if let content {
            let parsed = try? HTMLDocument(data: content.data(using: .utf8) ?? Data())
            return parsed?.root?.stringValue.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        }
        return ""
    }
}
