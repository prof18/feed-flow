import Foundation
import XCTest

@MainActor
final class WebsiteProfileIdentityTests: XCTestCase {
    func testHTTPSOriginNormalizesHostAndDefaultPort() {
        XCTAssertEqual(origin("https://Fixture.TEST/feed.xml?page=1"), "https://fixture.test:443")
    }

    func testExplicitPortIsKept() {
        XCTAssertEqual(origin("https://fixture.test:8443/feed"), "https://fixture.test:8443")
    }

    func testSchemeIsCaseInsensitive() {
        XCTAssertEqual(origin("HTTPS://fixture.test/feed"), "https://fixture.test:443")
    }

    func testNonHTTPSSchemesHaveNoOrigin() {
        for value in ["http://fixture.test/feed", "ftp://fixture.test/feed", "file:///tmp/feed.xml"] {
            XCTAssertNil(origin(value), value)
        }
    }

    func testCredentialsInURLHaveNoOrigin() {
        XCTAssertNil(origin("https://user@fixture.test/feed"))
        XCTAssertNil(origin("https://user:secret@fixture.test/feed"))
    }

    func testProfileIdentifierRequiresPrefixAndUUID() {
        let uuid = UUID()
        XCTAssertEqual(
            WebsiteProfileIdentity.identifier(profileId: "feedflow-recovery-\(uuid.uuidString)"),
            uuid
        )
        XCTAssertNil(WebsiteProfileIdentity.identifier(profileId: uuid.uuidString))
        XCTAssertNil(WebsiteProfileIdentity.identifier(profileId: "feedflow-recovery-not-a-uuid"))
    }

    private func origin(_ value: String) -> String? {
        URL(string: value).flatMap { WebsiteProfileIdentity.origin($0) }
    }
}
