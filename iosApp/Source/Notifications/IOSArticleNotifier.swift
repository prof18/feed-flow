import FeedFlowKit
import Foundation
import UserNotifications

final class IOSArticleNotifier {
    private let strings: FeedFlowStrings
    private let authorization: () async -> UNAuthorizationStatus
    private let loadAttachment: (String?, Int64) async throws -> URL?
    private let add: (UNNotificationRequest) async throws -> Void
    private let monotonicSeconds: () -> Double

    init(
        strings: FeedFlowStrings,
        authorization: @escaping () async -> UNAuthorizationStatus = {
            await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
        },
        // One loader per notifier, so its orphan cleanup runs once per batch, not per article.
        loadAttachment: @escaping (String?, Int64) async throws -> URL? = { [loader = NotificationImageAttachmentLoader()] in
            try await loader.load(urlString: $0, allowanceMillis: $1)
        },
        add: @escaping (UNNotificationRequest) async throws -> Void = {
            try await UNUserNotificationCenter.current().add($0)
        },
        monotonicSeconds: @escaping () -> Double = { ProcessInfo.processInfo.systemUptime }
    ) {
        self.strings = strings
        self.authorization = authorization
        self.loadAttachment = loadAttachment
        self.add = add
        self.monotonicSeconds = monotonicSeconds
    }

    func showArticleNotifications(
        batch: ArticleNotificationBatch,
        acknowledge: ([String]) async throws -> Void
    ) async throws {
        try Task.checkCancellation()
        let status = await authorization()
        guard status == .authorized || status == .provisional else { return }
        var submitted = false
        var imageElapsed: Double = 0
        let policy = ArticleNotificationPolicy.shared
        for article in batch.articles {
            try Task.checkCancellation()
            let remaining = max(0, policy.IMAGE_BATCH_BUDGET_MS - Int64(imageElapsed * 1_000))
            let allowance = min(policy.IMAGE_TIMEOUT_MS, remaining)
            let staged: URL?
            if allowance > 0 {
                let start = monotonicSeconds()
                do {
                    staged = try await loadAttachment(article.imageUrl, allowance)
                } catch {
                    try Task.checkCancellation()
                    if error is CancellationError { throw error }
                    staged = nil
                }
                imageElapsed += max(0, monotonicSeconds() - start)
            } else { staged = nil }
            defer { if let staged { try? FileManager.default.removeItem(at: staged) } }
            try Task.checkCancellation()
            let content = articleContent(article, silent: submitted)
            if let staged, let attachment = try? UNNotificationAttachment(identifier: article.id, url: staged) {
                content.attachments = [attachment]
            }
            if try await submit(identifier: policy.ARTICLE_ID_PREFIX + article.id, content: content) {
                submitted = true
                try await acknowledge([article.id])
            }
        }
        if !batch.remainderIds.isEmpty {
            try Task.checkCancellation()
            let content = UNMutableNotificationContent()
            content.title = strings.newArticlesNotificationTitle
            content.body = strings.notificationArticleOverflowBody(String(batch.remainderIds.count))
            content.sound = submitted ? nil : .default
            if try await submit(identifier: policy.OVERFLOW_ID, content: content) {
                try await acknowledge(batch.remainderIds)
            }
        }
    }

    private func articleContent(_ article: ArticleToNotify, silent: Bool) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        let title = article.title?.trimmingCharacters(in: .whitespacesAndNewlines)
        content.title = title.flatMap { $0.isEmpty ? nil : $0 } ?? strings.newArticlesNotificationTitle
        content.body = article.feedSourceTitle
        content.sound = silent ? nil : .default
        var components = URLComponents()
        components.scheme = "feedflow"
        components.host = "feed"
        var allowed = CharacterSet.urlPathAllowed
        allowed.remove(charactersIn: "/%")
        if let segment = article.id.addingPercentEncoding(withAllowedCharacters: allowed) {
            components.percentEncodedPath = "/" + segment
            if let url = components.url { content.userInfo = ["url": url.absoluteString] }
        }
        return content
    }

    private func submit(identifier: String, content: UNMutableNotificationContent) async throws -> Bool {
        do {
            try Task.checkCancellation()
            try await add(UNNotificationRequest(identifier: identifier, content: content, trigger: nil))
            return true
        } catch {
            try Task.checkCancellation()
            if error is CancellationError { throw error }
            guard !content.attachments.isEmpty else { return false }
        }
        content.attachments = []
        do {
            try Task.checkCancellation()
            try await add(UNNotificationRequest(identifier: identifier, content: content, trigger: nil))
            return true
        } catch {
            try Task.checkCancellation()
            if error is CancellationError { throw error }
            return false
        }
    }
}
