import Foundation
import XCTest

@MainActor
final class NotificationDeepLinkStoreTests: XCTestCase {
    func testConsumptionDismissesOnlyTheReceivingSceneBeforeRouting() throws {
        let widgetLinks = NotificationDeepLinkStore()
        let otherWidgetLinks = NotificationDeepLinkStore()
        let notificationLinks = NotificationDeepLinkStore()
        var dismissalCount = 0
        var otherDismissalCount = 0
        let observer = NotificationCenter.default.addObserver(
            forName: .didReceiveNotificationDeepLink, object: widgetLinks.dismissalSender, queue: .main
        ) { _ in
            MainActor.assumeIsolated {
                XCTAssertNil(widgetLinks.pendingURL)
                XCTAssertNil(notificationLinks.pendingURL)
                dismissalCount += 1
            }
        }
        let otherObserver = NotificationCenter.default.addObserver(
            forName: .didReceiveNotificationDeepLink, object: otherWidgetLinks.dismissalSender, queue: .main
        ) { _ in
            MainActor.assumeIsolated {
                otherDismissalCount += 1
            }
        }
        defer {
            NotificationCenter.default.removeObserver(observer)
            NotificationCenter.default.removeObserver(otherObserver)
        }

        let widgetURL = try XCTUnwrap(URL(string: "feedflow://feed/article"))
        let otherWidgetURL = try XCTUnwrap(URL(string: "feedflow://feed/other-article"))
        let notificationURL = try XCTUnwrap(URL(string: "feedflow://category/category"))
        widgetLinks.enqueue(widgetURL)
        otherWidgetLinks.enqueue(otherWidgetURL)
        XCTAssertEqual(dismissalCount, 0)
        XCTAssertEqual(widgetLinks.consume(), widgetURL)
        XCTAssertEqual(dismissalCount, 1)

        notificationLinks.enqueue(notificationURL)
        XCTAssertEqual(notificationLinks.consume(dismissingSheetsIn: widgetLinks), notificationURL)
        XCTAssertNil(widgetLinks.consume())
        XCTAssertNil(notificationLinks.consume(dismissingSheetsIn: widgetLinks))
        XCTAssertEqual(dismissalCount, 2)
        XCTAssertEqual(otherDismissalCount, 0)
        XCTAssertEqual(otherWidgetLinks.pendingURL, otherWidgetURL)
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
