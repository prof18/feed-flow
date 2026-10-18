import Foundation
import XCTest

final class WebsiteClearanceCookiesTests: XCTestCase {
    func testOnlyClearanceAndApprovedUserAgentAreReplayed() throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/articles/one"))
        let cookies = try [cookie(name: "cf_clearance"), cookie(name: "login", value: "private")]
        let identity = try XCTUnwrap(WebsiteClearanceCookies.identity(cookies: cookies, url: url, userAgent: "Browser UA"))
        XCTAssertEqual(identity.cookie, "cf_clearance=fixture")
        XCTAssertEqual(identity.userAgent, "Browser UA")
    }

    func testScopeAndExpiryDoNotLeakClearance() throws {
        let clearance = try cookie(path: "/articles", expires: Date().addingTimeInterval(60))
        for endpoint in ["https://fixture.test/articles-other", "https://other.test/articles", "http://fixture.test/articles"] {
            let url = try XCTUnwrap(URL(string: endpoint))
            XCTAssertNil(WebsiteClearanceCookies.identity(cookies: [clearance], url: url, userAgent: "UA")?.cookie)
        }
        let url = try XCTUnwrap(URL(string: "https://fixture.test/articles/two"))
        XCTAssertNotNil(WebsiteClearanceCookies.identity(cookies: [clearance], url: url, userAgent: "UA")?.cookie)
        let expired = try cookie(expires: Date().addingTimeInterval(-60))
        XCTAssertNil(WebsiteClearanceCookies.identity(cookies: [expired], url: url, userAgent: "UA")?.cookie)
    }

    func testAmbiguousCookiesCannotProduceReplayIdentity() throws {
        let url = try XCTUnwrap(URL(string: "https://fixture.test/articles"))
        let cookies = try [cookie(), cookie(path: "/articles", value: "second")]
        XCTAssertNil(WebsiteClearanceCookies.identity(cookies: cookies, url: url, userAgent: "UA"))
    }

    private func cookie(
        name: String = "cf_clearance", path: String = "/", value: String = "fixture", expires: Date? = nil
    ) throws -> HTTPCookie {
        var properties: [HTTPCookiePropertyKey: Any] = [
            .name: name, .value: value, .domain: "fixture.test", .path: path, .secure: "TRUE",
        ]
        properties[.expires] = expires
        return try XCTUnwrap(HTTPCookie(properties: properties))
    }
}
