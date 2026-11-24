import FeedFlowKit
import UserNotifications
import UIKit
import XCTest

final class IOSArticleNotifierTests: XCTestCase {
    private var requests: [UNNotificationRequest] = []
    private var acknowledged: [[String]] = []

    func testPayloadsStableIDsAndEncodedURL() async throws {
        let sender = makeSender()
        let articles = [article("a/b", title: "  Title  "), article("blank", title: " \n")]
        try await sender.showArticleNotifications(articles: articles) { self.acknowledged.append($0) }
        XCTAssertEqual(requests.map(\.identifier), ["article-a/b", "article-blank"])
        XCTAssertEqual(requests[0].content.title, "Title")
        XCTAssertEqual(requests[0].content.body, "Source")
        XCTAssertEqual(requests[0].content.userInfo["url"] as? String, "feedflow://feed/a%2Fb")
        // The watcher reads the tag back to decide when the article's read state retires it.
        XCTAssertEqual(requests[0].content.userInfo[NotificationTag.shared.USER_INFO_KEY] as? String, "article-a/b")
        XCTAssertNil(requests[0].trigger)
        XCTAssertNotNil(requests[0].content.sound)
        XCTAssertNil(requests[1].content.sound)
        XCTAssertEqual(requests[1].content.title, EnFeedFlowStringsKt.EnFeedFlowStrings.newArticlesNotificationTitle)
        XCTAssertEqual(acknowledged, [["a/b"], ["blank"]])
    }

    func testDeniedAndEmptyBatchesDoNotSubmit() async throws {
        let denied = makeSender(authorization: { .denied })
        try await denied.showArticleNotifications(articles: articles("a")) { _ in XCTFail("denied ack") }
        try await makeSender().showArticleNotifications(
            articles: []
        ) { _ in XCTFail("empty ack") }
        XCTAssertTrue(requests.isEmpty)
    }

    func testPostFailuresLeavePendingAndFirstSuccessOwnsSound() async throws {
        let sender = makeSender(add: { request in
            if request.identifier == "article-a" { throw TestError.failed }
            self.requests.append(request)
        })
        try await sender.showArticleNotifications(articles: articles("a", "b", "c")) { self.acknowledged.append($0) }
        XCTAssertEqual(acknowledged, [["b"], ["c"]])
        XCTAssertNotNil(requests[0].content.sound)
        XCTAssertNil(requests[1].content.sound)
    }

    func testAcknowledgementErrorStopsBatch() async {
        do {
            try await makeSender().showArticleNotifications(articles: articles("a", "b")) { _ in
                throw TestError.failed
            }
            XCTFail("must throw")
        } catch {
            XCTAssertEqual(requests.count, 1)
        }
    }

    func testCancellationStopsSubmissionAndAcknowledgement() async {
        let sender = makeSender(add: { _ in throw CancellationError() })
        do {
            try await sender.showArticleNotifications(articles: articles("a", "b")) { _ in XCTFail("cancelled ack") }
            XCTFail("must cancel")
        } catch {
            XCTAssertTrue(error is CancellationError)
            XCTAssertTrue(requests.isEmpty)
        }
    }

    func testImageBudgetExcludesSubmissionTime() async throws {
        var time: Double = 0
        var allowances: [Int64] = []
        let sender = IOSArticleNotifier(
            strings: EnFeedFlowStringsKt.EnFeedFlowStrings,
            authorization: { .provisional },
            loadAttachment: { _, allowance in allowances.append(allowance); time += 1; return nil },
            add: { request in self.requests.append(request); time += 100 },
            monotonicSeconds: { time }
        )
        try await sender.showArticleNotifications(articles: articles("1", "2", "3", "4", "5", "6")) { _ in }
        XCTAssertEqual(allowances, [1000, 1000, 1000, 1000, 1000])
        XCTAssertEqual(requests.count, 6)
    }

    func testAttachmentSurvivesAddAndIsRemovedAfterTextRetry() async throws {
        let url = try temporaryJPEG()
        var attempts = 0
        let sender = makeSender(load: { _, _ in url }, add: { request in
            attempts += 1
            XCTAssertTrue(FileManager.default.fileExists(atPath: url.path))
            if attempts == 1 {
                XCTAssertEqual(request.content.attachments.count, 1)
                throw TestError.failed
            }
            XCTAssertTrue(request.content.attachments.isEmpty)
            self.requests.append(request)
        })
        try await sender.showArticleNotifications(articles: articles("a")) { self.acknowledged.append($0) }
        XCTAssertEqual(attempts, 2)
        XCTAssertEqual(acknowledged, [["a"]])
        XCTAssertFalse(FileManager.default.fileExists(atPath: url.path))
    }

    func testAttachmentCleanupOnBothPostFailuresAndCancellation() async throws {
        for cancellation in [false, true] {
            let url = try temporaryJPEG()
            let sender = makeSender(load: { _, _ in url }, add: { _ in
                if cancellation { throw CancellationError() }
                throw TestError.failed
            })
            do {
                try await sender.showArticleNotifications(articles: articles("a")) { _ in XCTFail("failure ack") }
                XCTAssertFalse(cancellation)
            } catch { XCTAssertTrue(cancellation) }
            XCTAssertFalse(FileManager.default.fileExists(atPath: url.path))
        }
    }

    private func makeSender(
        authorization: @escaping () async -> UNAuthorizationStatus = { .authorized },
        load: @escaping (String?, Int64) async throws -> URL? = { _, _ in nil },
        add: ((UNNotificationRequest) async throws -> Void)? = nil
    ) -> IOSArticleNotifier {
        IOSArticleNotifier(
            strings: EnFeedFlowStringsKt.EnFeedFlowStrings, authorization: authorization, loadAttachment: load,
            add: add ?? { self.requests.append($0) }
        )
    }

    private func article(_ id: String, title: String? = "Title") -> ArticleToNotify {
        ArticleToNotify(id: id, title: title, feedSourceTitle: "Source", imageUrl: "https://example.com/image.jpg")
    }

    private func articles(_ ids: String...) -> [ArticleToNotify] {
        ids.map { article($0) }
    }

    private func temporaryJPEG() throws -> URL {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".jpg")
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 8, height: 4))
        let image = renderer.image { _ in UIColor.red.setFill(); UIRectFill(CGRect(x: 0, y: 0, width: 8, height: 4)) }
        try XCTUnwrap(image.jpegData(compressionQuality: 0.8)).write(to: url)
        return url
    }
}
