import FeedFlowKit
import Foundation

/// Rewrites an article URL into the deep link that opens it in a third-party browser.
enum BrowserURLBuilder {
    enum BrowserId {
        static let defaultBrowser = ""
        static let chrome = "googlechromes://"
        static let firefox = "firefox://open-url?url="
        static let firefoxFocus = "firefox-focus://open-url?url="
        static let duckDuckGo = "ddgQuickLink://"
        static let brave = "brave://open-url?url="
    }

    static func url(browserId: String?, stringUrl: String) -> URL? {
        guard let url = URL(string: stringUrl) else {
            return nil
        }

        guard let browserId else {
            return url
        }

        switch browserId {
        case BrowserIds.shared.IN_APP_BROWSER, BrowserId.defaultBrowser:
            return url
        case BrowserId.chrome:
            return makeChromeURL(url: url)
        case BrowserId.firefox:
            return makeQueryBrowserURL(scheme: "firefox", targetUrl: stringUrl)
        case BrowserId.firefoxFocus:
            return makeQueryBrowserURL(scheme: "firefox-focus", targetUrl: stringUrl)
        case BrowserId.duckDuckGo:
            return makeQuickLinkBrowserURL(url: url, scheme: "ddgQuickLink")
        case BrowserId.brave:
            return makeQueryBrowserURL(scheme: "brave", targetUrl: stringUrl)
        default:
            return url
        }
    }

    private static func makeChromeURL(url: URL) -> URL? {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }

        switch url.scheme {
        case "https":
            components.scheme = "googlechromes"
        case "http":
            components.scheme = "googlechrome"
        default:
            return nil
        }

        return components.url
    }

    private static func makeQueryBrowserURL(scheme: String, targetUrl: String) -> URL? {
        guard let encodedTargetUrl = targetUrl.addingPercentEncoding(
            withAllowedCharacters: CharacterSet.urlBrowserQueryAllowed
        ) else {
            return nil
        }

        return URL(string: "\(scheme)://open-url?url=\(encodedTargetUrl)")
    }

    private static func makeQuickLinkBrowserURL(url: URL, scheme: String) -> URL? {
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return nil
        }

        switch url.scheme {
        case "https", "http":
            components.scheme = scheme
            return components.url
        default:
            return nil
        }
    }
}

private extension CharacterSet {
    static let urlBrowserQueryAllowed: CharacterSet = {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-._~")
        return allowed
    }()
}
