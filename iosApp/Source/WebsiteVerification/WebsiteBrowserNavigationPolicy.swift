import Foundation

enum WebsiteBrowserNavigationPolicy {
    static func allows(
        url: URL,
        isMainFrame: Bool,
        approvedOrigin: String?,
        origin: (URL) -> String?
    ) -> Bool {
        if isMainFrame {
            guard let approvedOrigin else { return false }
            return origin(url) == approvedOrigin
        }
        if url.absoluteString == "about:blank" || url.absoluteString == "about:srcdoc" {
            return true
        }
        return url.scheme?.lowercased() == "https" && origin(url) != nil
    }
}
