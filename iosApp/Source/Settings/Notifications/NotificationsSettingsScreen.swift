//
//  NotificationsSettingsScreen.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 14/09/24.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import UserNotifications

struct NotificationsSettingsScreen: View {
    @Environment(\.dismiss)
    private var dismiss
    @Environment(\.openURL)
    private var openURL

    @StateObject private var vmStoreOwner = VMStoreOwner<NotificationsViewModel>(
        Deps.shared.getNotificationsViewModel()
    )

    @State private var notificationState = NotificationSettingState(
        feedSources: [],
        isEnabledForAll: false
    )

    @State private var hasNotificationPermission: Bool = false
    @State private var permissionStatus: UNAuthorizationStatus = .notDetermined

    var body: some View {
        NotificationsSettingsContent(
            notificationState: notificationState,
            hasNotificationPermission: hasNotificationPermission,
            permissionStatus: permissionStatus,
            onNavigateBack: { dismiss() },
            onAllNotificationsToggle: { status in
                vmStoreOwner.instance.updateAllNotificationStatus(status: status)
            },
            onFeedSourceNotificationsToggle: { feedSourceId, status in
                vmStoreOwner.instance.updateNotificationStatus(status: status, feedSourceId: feedSourceId)
            },
            onRequestPermissions: {
                requestNotificationPermissions()
            },
            onOpenSettings: {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    openURL(url)
                }
            }
        )
        .task {
            for await state in vmStoreOwner.instance.notificationSettingState {
                self.notificationState = state
            }
        }
        .onAppear {
            checkNotificationPermission()
        }
    }

    private func checkNotificationPermission() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.permissionStatus = settings.authorizationStatus
                self.hasNotificationPermission = (settings.authorizationStatus == .authorized ||
                    settings.authorizationStatus == .provisional)
            }
        }
    }

    private func requestNotificationPermissions() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            DispatchQueue.main.async {
                if granted {
                    self.hasNotificationPermission = true
                    self.permissionStatus = .authorized
                } else {
                    if let error = error {
                        print("Error requesting notification permissions: \(error.localizedDescription)")
                    }
                }
            }
        }
    }
}
