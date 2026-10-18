import Foundation
import XCTest

final class WebsiteBrowserNavigationPolicyTests: XCTestCase {
    func testTurnstileLocalFramesAreAllowedOnlyAsSubframes() throws {
        for value in ["about:blank", "about:srcdoc"] {
            let url = try XCTUnwrap(URL(string: value))
            XCTAssertTrue(allows(url, isMainFrame: false))
            XCTAssertFalse(allows(url, isMainFrame: true))
        }
    }

    func testMainPageRemainsRestrictedToApprovedOrigin() throws {
        XCTAssertTrue(try allows(XCTUnwrap(URL(string: "https://fixture.test/feed")), isMainFrame: true))
        XCTAssertFalse(try allows(XCTUnwrap(URL(string: "https://other.test/feed")), isMainFrame: true))
    }

    func testHTTPSChallengeSubframesAreAllowed() throws {
        XCTAssertTrue(try allows(
            XCTUnwrap(URL(string: "https://challenges.cloudflare.com/turnstile")), isMainFrame: false
        ))
    }

    func testOtherLocalAndInsecureFramesRemainBlocked() throws {
        for value in ["about:config", "file:///tmp/page.html", "http://fixture.test/frame", "data:text/html,test"] {
            XCTAssertFalse(try allows(XCTUnwrap(URL(string: value)), isMainFrame: false))
        }
    }

    func testMainPageCannotOpenBeforeOriginApproval() throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/feed"))
        XCTAssertFalse(WebsiteBrowserNavigationPolicy.allows(
            url: url, isMainFrame: true, approvedOrigin: nil, origin: origin
        ))
    }

    private func allows(_ url: URL, isMainFrame: Bool) -> Bool {
        WebsiteBrowserNavigationPolicy.allows(
            url: url, isMainFrame: isMainFrame, approvedOrigin: "https://fixture.test:443", origin: origin
        )
    }

    private func origin(_ url: URL) -> String? {
        guard url.scheme == "https", let host = url.host else { return nil }
        return "https://\(host):\(url.port ?? 443)"
    }
}
