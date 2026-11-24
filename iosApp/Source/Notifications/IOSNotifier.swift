import FeedFlowKit
import Foundation
import UserNotifications

class IOSNotifier: Notifier {
    private let notificationCenter = UNUserNotificationCenter.current()

    func showNewArticlesNotification(feedSourcesToNotify: [FeedSourceToNotify]) -> Bool {
        guard !feedSourcesToNotify.isEmpty else { return false }

        let settingsRepository = Deps.shared.getSettingsRepository()
        let mode = settingsRepository.getNotificationMode()
        let tags = NotificationTag.shared

        switch mode {
        case .article:
            return false

        case .feedSource:
            for source in feedSourcesToNotify {
                scheduleNotification(
                    identifier: makeNotificationIdentifier(prefix: "source", key: source.feedSourceId),
                    tag: tags.forFeedSource(feedSourceId: source.feedSourceId),
                    title: feedFlowStrings.newArticlesNotificationTitle,
                    body: source.feedSourceTitle,
                    url: "feedflow://feedsourcefilter/\(source.feedSourceId)"
                )
            }

        case .category:
            // Grouped by id, not title: two categories can share a title, and the tag a
            // notification carries decides which category's read state retires it.
            let grouped = Dictionary(grouping: feedSourcesToNotify) { $0.categoryId }
            for (categoryId, sources) in grouped {
                let categoryTitle = sources.first?.categoryTitle
                let body: String
                if let categoryTitle {
                    body = feedFlowStrings.notificationCategoryBody(categoryTitle)
                } else {
                    body = feedFlowStrings.notificationGroupedBody
                }
                let url = categoryId.map { "feedflow://category/\($0)" }
                scheduleNotification(
                    identifier: makeNotificationIdentifier(
                        prefix: "category",
                        key: categoryId ?? categoryTitle ?? "uncategorized"
                    ),
                    tag: tags.forCategory(categoryId: categoryId),
                    title: feedFlowStrings.newArticlesNotificationTitle,
                    body: body,
                    url: url
                )
            }

        case .grouped:
            scheduleNotification(
                identifier: makeNotificationIdentifier(prefix: "grouped", key: "all"),
                tag: tags.GROUPED,
                title: feedFlowStrings.newArticlesNotificationTitle,
                body: feedFlowStrings.notificationGroupedBody,
                url: nil
            )
        }

        return true
    }

    private func scheduleNotification(identifier: String, tag: String, title: String, body: String, url: String?) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        var userInfo: [String: Any] = [NotificationTag.shared.USER_INFO_KEY: tag]
        if let url {
            userInfo["url"] = url
        }
        content.userInfo = userInfo

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        notificationCenter.add(request) { error in
            if let error {
                print("IOSNotifier: error scheduling notification: \(error.localizedDescription)")
            }
        }
    }

    private func makeNotificationIdentifier(prefix: String, key: String) -> String {
        "\(prefix)-\(key)-\(UUID().uuidString)"
    }
}
