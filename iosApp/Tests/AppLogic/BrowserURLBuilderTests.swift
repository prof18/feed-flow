import FeedFlowKit
import Foundation
import XCTest

final class BrowserURLBuilderTests: XCTestCase {
    private let article = "https://example.com/post?id=1&ref=feed#top"

    func testInAppAndDefaultBrowserKeepTheArticleURL() {
        for browserId in [nil, BrowserIds.shared.IN_APP_BROWSER, BrowserURLBuilder.BrowserId.defaultBrowser] {
            XCTAssertEqual(url(browserId, article), article)
        }
    }

    func testUnknownBrowserKeepsTheArticleURL() {
        XCTAssertEqual(url("removed-browser://", article), article)
    }

    func testChromeSwapsTheSchemeKeepingSecurity() {
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.chrome, article),
            "googlechromes://example.com/post?id=1&ref=feed#top"
        )
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.chrome, "http://example.com/post"),
            "googlechrome://example.com/post"
        )
    }

    func testChromeCannotOpenNonWebURLs() {
        XCTAssertNil(url(BrowserURLBuilder.BrowserId.chrome, "mailto:someone@example.com"))
    }

    func testQueryBrowsersPercentEncodeTheWholeArticleURL() {
        let encoded = "https%3A%2F%2Fexample.com%2Fpost%3Fid%3D1%26ref%3Dfeed%23top"
        XCTAssertEqual(url(BrowserURLBuilder.BrowserId.firefox, article), "firefox://open-url?url=\(encoded)")
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.firefoxFocus, article),
            "firefox-focus://open-url?url=\(encoded)"
        )
        XCTAssertEqual(url(BrowserURLBuilder.BrowserId.brave, article), "brave://open-url?url=\(encoded)")
    }

    func testQueryBrowsersKeepUnreservedCharacters() {
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.firefox, "https://example.com/a-b_c.d~e"),
            "firefox://open-url?url=https%3A%2F%2Fexample.com%2Fa-b_c.d~e"
        )
    }

    func testDuckDuckGoUsesQuickLinkSchemeForWebURLsOnly() {
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.duckDuckGo, article),
            "ddgQuickLink://example.com/post?id=1&ref=feed#top"
        )
        XCTAssertEqual(
            url(BrowserURLBuilder.BrowserId.duckDuckGo, "http://example.com/post"),
            "ddgQuickLink://example.com/post"
        )
        XCTAssertNil(url(BrowserURLBuilder.BrowserId.duckDuckGo, "mailto:someone@example.com"))
    }

    func testUnparseableArticleURLHasNoBrowserURL() {
        XCTAssertNil(url(BrowserURLBuilder.BrowserId.chrome, ""))
    }

    private func url(_ browserId: String?, _ stringUrl: String) -> String? {
        BrowserURLBuilder.url(browserId: browserId, stringUrl: stringUrl)?.absoluteString
    }
}
