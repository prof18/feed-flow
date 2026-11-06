import Foundation

/// Returns a URL the share sheet can use, or nil for URL-less items and links that don't parse.
enum ShareableURL {
    static func make(from string: String?) -> URL? {
        guard let trimmed = string?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty,
              let url = URL(string: trimmed),
              url.scheme != nil,
              url.host?.isEmpty == false else {
            return nil
        }
        return url
    }
}
