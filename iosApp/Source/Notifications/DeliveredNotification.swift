import FeedFlowKit
import Foundation
import UserNotifications

/// A delivered notification, reduced to which one to remove and whose read state retires it.
struct DeliveredNotification {
    let identifier: String
    let tag: String

    init(identifier: String, tag: String) {
        self.identifier = identifier
        self.tag = tag
    }

    /// Nil for a notification without a tag: one this app didn't post as a new-article notification,
    /// or one an older version posted.
    init?(_ notification: UNNotification) {
        guard let tag = notification.request.content.userInfo[NotificationTag.shared.USER_INFO_KEY] as? String
        else { return nil }
        identifier = notification.request.identifier
        self.tag = tag
    }
}
