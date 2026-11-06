import Foundation
import XCTest

final class ShareableURLTests: XCTestCase {
    func testWebURLIsShareable() {
        XCTAssertEqual(
            ShareableURL.make(from: "https://example.com/post?id=1")?.absoluteString,
            "https://example.com/post?id=1"
        )
    }

    func testSurroundingWhitespaceIsTrimmed() {
        XCTAssertEqual(
            ShareableURL.make(from: "  https://example.com/post \n")?.absoluteString,
            "https://example.com/post"
        )
    }

    func testMissingOrBlankURLIsNotShareable() {
        XCTAssertNil(ShareableURL.make(from: nil))
        XCTAssertNil(ShareableURL.make(from: ""))
        XCTAssertNil(ShareableURL.make(from: "   "))
    }

    func testURLWithoutSchemeOrHostIsNotShareable() {
        XCTAssertNil(ShareableURL.make(from: "not a url"))
        XCTAssertNil(ShareableURL.make(from: "/relative/path"))
        XCTAssertNil(ShareableURL.make(from: "https://"))
    }
}
