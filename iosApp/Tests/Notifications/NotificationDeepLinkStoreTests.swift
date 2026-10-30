import Foundation
import XCTest

@MainActor
final class NotificationDeepLinkStoreTests: XCTestCase {
    func testConsumptionDismissesPresentedSheetsBeforeRoutingExactlyOnce() throws {
        let store = NotificationDeepLinkStore()
        var dismissalCount = 0
        let observer = NotificationCenter.default.addObserver(
            forName: .didReceiveNotificationDeepLink, object: nil, queue: .main
        ) { _ in
            MainActor.assumeIsolated {
                XCTAssertNil(store.pendingURL)
                dismissalCount += 1
            }
        }
        defer { NotificationCenter.default.removeObserver(observer) }
        let url = try XCTUnwrap(URL(string: "feedflow://feed/article"))
        store.enqueue(url)
        XCTAssertEqual(dismissalCount, 0)
        XCTAssertEqual(store.consume(), url)
        XCTAssertEqual(dismissalCount, 1)
        XCTAssertNil(store.consume())
        XCTAssertEqual(dismissalCount, 1)
    }

    func testTapBeforeViewExistsIsConsumedExactlyOnce() throws {
        let store = NotificationDeepLinkStore()
        let url = try XCTUnwrap(URL(string: "feedflow://feed/a%2Fb"))
        store.enqueue(url)
        XCTAssertEqual(store.pendingURL, url)
        XCTAssertEqual(store.consume(), url)
        XCTAssertNil(store.pendingURL)
        XCTAssertNil(store.consume())
    }

    func testNewerTapReplacesPendingAndInvalidURLCannotReplaceIt() throws {
        let store = NotificationDeepLinkStore()
        let first = try XCTUnwrap(URL(string: "feedflow://feedsourcefilter/source"))
        let latest = try XCTUnwrap(URL(string: "feedflow://category/category"))
        store.enqueue(first)
        store.enqueue(latest)
        for invalid in ["https://feed/a", "feedflow://unknown/a", "feedflow://feed/", "feedflow://feed", "feedflow://feed/a/b"] {
            store.enqueue(try XCTUnwrap(URL(string: invalid)))
        }
        XCTAssertEqual(store.consume(), latest)
        XCTAssertNil(store.pendingURL)
    }
}
