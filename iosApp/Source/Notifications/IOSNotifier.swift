import FeedFlowKit
import Foundation
import UserNotifications

class IOSNotifier: Notifier {
    private let notificationCenter = UNUserNotificationCenter.current()

    func showNewArticlesNotification(feedSourcesToNotify: [FeedSourceToNotify]) -> Bool {
        guard !feedSourcesToNotify.isEmpty else { return false }

        let settingsRepository = Deps.shared.getSettingsRepository()
        let mode = settingsRepository.getNotificationMode()

        switch mode {
        case .feedSource:
            for (index, source) in feedSourcesToNotify.enumerated() {
                scheduleNotification(
                    identifier: "feedflow-source-\(index)",
                    title: feedFlowStrings.newArticlesNotificationTitle,
                    body: source.feedSourceTitle,
                    url: "feedflow://feedsourcefilter/\(source.feedSourceId)"
                )
            }

        case .category:
            let grouped = Dictionary(grouping: feedSourcesToNotify) { $0.categoryTitle as String? }
            for (index, (categoryTitle, sources)) in grouped.enumerated() {
                let categoryId = sources.first?.categoryId
                let body: String
                if let title = categoryTitle {
                    body = feedFlowStrings.notificationCategoryBody(title)
                } else {
                    body = feedFlowStrings.notificationGroupedBody
                }
                let url = categoryId.map { "feedflow://category/\($0)" }
                scheduleNotification(
                    identifier: "feedflow-category-\(index)",
                    title: feedFlowStrings.newArticlesNotificationTitle,
                    body: body,
                    url: url
                )
            }

        case .grouped:
            scheduleNotification(
                identifier: "feedflow-grouped",
                title: feedFlowStrings.newArticlesNotificationTitle,
                body: feedFlowStrings.notificationGroupedBody,
                url: nil
            )
        }

        return true
    }

    private func scheduleNotification(identifier: String, title: String, body: String, url: String?) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        if let url {
            content.userInfo = ["url": url]
        }

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        notificationCenter.add(request) { error in
            if let error {
                print("IOSNotifier: error scheduling notification: \(error.localizedDescription)")
            }
        }
    }
}
