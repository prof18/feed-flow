//
//  IOSNotifier.swift
//  FeedFlow
//
//  Created by [Your Name] on [Date].
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import UserNotifications

class IOSNotifier: Notifier {
    private let notificationCenter = UNUserNotificationCenter.current()

    func showNewArticlesNotification(feedSourcesToNotify: [FeedSourceToNotify]) {
        for sourceToNotify in feedSourcesToNotify {
            let content = UNMutableNotificationContent()
            content.title = feedFlowStrings.newArticlesNotificationTitle
            content.body = sourceToNotify.feedSourceTitle
            content.sound = UNNotificationSound.default

            content.userInfo = [
                "feedSourceId": sourceToNotify.feedSourceId
            ]

            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)

            let requestIdentifier = "feedflow-notification-\(sourceToNotify.feedSourceId)"
            let request = UNNotificationRequest(
                identifier: requestIdentifier,
                content: content,
                trigger: trigger
            )

            notificationCenter.add(request) { error in
                if let error = error {
                    print("Error adding notification request: \(error.localizedDescription)")
                }
            }
        }
    }
}
