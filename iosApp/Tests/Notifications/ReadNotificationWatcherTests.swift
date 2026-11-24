import FeedFlowKit
import UserNotifications
import XCTest

@MainActor
final class ReadNotificationWatcherTests: XCTestCase {
    private var removed: [[String]] = []
    private var asked: [[String]] = []

    func testRemovesEveryNotificationCarryingAStaleTag() async {
        let posted = [
            delivered("a", tags.forArticle(articleId: "1")),
            delivered("b", tags.forArticle(articleId: "2")),
            delivered("c", tags.forFeedSource(feedSourceId: "feed")),
            delivered("d", tags.forFeedSource(feedSourceId: "feed")),
        ]
        let watcher = makeWatcher(posted: posted) { _ in
            [self.tags.forArticle(articleId: "2"), self.tags.forFeedSource(feedSourceId: "feed")]
        }

        await watcher.dismissReadNotifications()

        XCTAssertEqual(asked, [["article-1", "article-2", "feed-source-feed", "feed-source-feed"]])
        XCTAssertEqual(removed, [["b", "c", "d"]])
    }

    func testAnEmptyTrayAsksNothing() async {
        let watcher = makeWatcher(posted: []) { _ in XCTFail("asked with an empty tray"); return [] }

        await watcher.dismissReadNotifications()

        XCTAssertTrue(asked.isEmpty)
        XCTAssertTrue(removed.isEmpty)
    }

    func testNothingStaleLeavesTheTrayAlone() async {
        let watcher = makeWatcher(posted: [delivered("a", tags.forArticle(articleId: "1"))]) { _ in [] }

        await watcher.dismissReadNotifications()

        XCTAssertEqual(asked, [["article-1"]])
        XCTAssertTrue(removed.isEmpty)
    }

    func testAFailedLookupLeavesTheTrayAlone() async {
        let watcher = makeWatcher(posted: [delivered("a", tags.forArticle(articleId: "1"))]) { _ in
            throw TestError.failed
        }

        await watcher.dismissReadNotifications()

        XCTAssertTrue(removed.isEmpty)
    }

    private var tags: NotificationTag { NotificationTag.shared }

    private func delivered(_ identifier: String, _ tag: String) -> DeliveredNotification {
        DeliveredNotification(identifier: identifier, tag: tag)
    }

    private func makeWatcher(
        posted: [DeliveredNotification],
        staleTags: @escaping ([String]) async throws -> [String]
    ) -> ReadNotificationWatcher {
        ReadNotificationWatcher(
            delivered: { posted },
            staleTags: { tags in
                self.asked.append(tags)
                return try await staleTags(tags)
            },
            remove: { self.removed.append($0) }
        )
    }
}
