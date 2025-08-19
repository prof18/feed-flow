//
//  NotificationsSettingsScreenContent.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 16/04/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import SwiftUI
import UserNotifications

struct NotificationsSettingsContent: View {
    let notificationState: NotificationSettingState
    let hasNotificationPermission: Bool
    let permissionStatus: UNAuthorizationStatus
    let onNavigateBack: () -> Void
    let onAllNotificationsToggle: (Bool) -> Void
    let onFeedSourceNotificationsToggle: (String, Bool) -> Void
    let onRequestPermissions: () -> Void
    let onOpenSettings: () -> Void

    var body: some View {
        NavigationStack {
            VStack {
                if notificationState.feedSources.isEmpty {
                    Text(feedFlowStrings.settingsNotificationNoFeed)
                        .font(.body)
                } else {
                    List {
                        if !hasNotificationPermission {
                            Section(content: {
                                HStack {
                                    Text(feedFlowStrings.settingsNotificationsPermissionStatusDenied)
                                        .foregroundColor(.red)
                                    Spacer()
                                }

                                if permissionStatus == .denied {
                                    Button(feedFlowStrings.settingsOpenSettings) {
                                        onOpenSettings()
                                    }
                                    .tint(.blue)

                                    Text(feedFlowStrings.settingsNotificationsSettingsInstructions)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                } else {
                                    Button(feedFlowStrings.settingsNotificationsPermissionRequestButton) {
                                        onRequestPermissions()
                                    }
                                    .tint(.blue)
                                }
                            }, header: {
                                Text(feedFlowStrings.settingsNotificationsPermissionStatusTitle)
                            })
                        }

                        Section {
                            Text(feedFlowStrings.settingsNotificationsWarningReliability)
                                .font(.footnote)
                                .foregroundColor(.secondary)
                                .padding(.vertical, Spacing.small)

                            Toggle(isOn: Binding(
                                get: { notificationState.isEnabledForAll },
                                set: { onAllNotificationsToggle($0) }
                            )) {
                                Text(feedFlowStrings.settingsNotificationsEnableAllTitle)
                            }
                        }

                        Section(content: {
                            ForEach(notificationState.feedSources, id: \.feedSourceId) { source in
                                Toggle(isOn: Binding(
                                    get: { source.isEnabled },
                                    set: { onFeedSourceNotificationsToggle(source.feedSourceId, $0) }
                                )) {
                                    Text(source.feedSourceTitle)
                                }
                            }
                        }, header: {
                            Text(feedFlowStrings.feedsTitle)
                        })
                    }
                }
            }
            .navigationTitle(feedFlowStrings.settingsNotificationsTitle)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
