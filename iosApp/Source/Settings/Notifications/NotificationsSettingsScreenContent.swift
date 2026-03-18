import FeedFlowKit
import SwiftUI
import UserNotifications

struct NotificationsSettingsScreenContent: View {
    let permissionStatus: UNAuthorizationStatus
    let notificationSettingState: NotificationSettingState
    let onRequestPermission: () -> Void
    let onOpenSettings: () -> Void
    let onEnableAllToggled: (Bool) -> Void
    let onFeedNotificationToggled: (String, Bool) -> Void
    let onNotificationModeChanged: (NotificationMode) -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        Form {
            if permissionStatus != .authorized && permissionStatus != .provisional {
                permissionSection
            }
            if permissionStatus == .authorized || permissionStatus == .provisional {
                warningSection
                notificationModeSection
                feedsSection
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }

    @ViewBuilder private var permissionSection: some View {
        Section {
            Text(feedFlowStrings.settingsNotificationsPermissionDescription)
                .foregroundStyle(.secondary)
        }

        Section {
            switch permissionStatus {
            case .denied:
                Button {
                    onOpenSettings()
                } label: {
                    Label(feedFlowStrings.settingsNotificationsOpenSettingsButton, systemImage: "bell.badge")
                }

            default:
                Button {
                    onRequestPermission()
                } label: {
                    Label(feedFlowStrings.settingsNotificationsPermissionRequestButton, systemImage: "bell.badge")
                }
            }
        }
    }

    private var warningSection: some View {
        Section {
            Text(feedFlowStrings.settingsNotificationsWarningReliability)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
    }

    private var notificationModeSection: some View {
        Section {
            Toggle(isOn: Binding(
                get: { notificationSettingState.isEnabledForAll },
                set: { onEnableAllToggled($0) }
            )) {
                Text(feedFlowStrings.settingsNotificationsEnableAllTitle)
            }
            .tint(.accentColor)

            Picker(selection: Binding(
                get: { notificationSettingState.notificationMode },
                set: { onNotificationModeChanged($0) }
            )) {
                Text(feedFlowStrings.settingsNotificationModeFeedSource)
                    .tag(NotificationMode.feedSource)
                Text(feedFlowStrings.settingsNotificationModeCategory)
                    .tag(NotificationMode.category)
                Text(feedFlowStrings.settingsNotificationModeGrouped)
                    .tag(NotificationMode.grouped)
            } label: {
                Text(feedFlowStrings.settingsNotificationMode)
            }
        } footer: {
            Text(feedFlowStrings.settingsNotificationModeDesc)
        }
    }

    @ViewBuilder private var feedsSection: some View {
        if notificationSettingState.feedSources.isEmpty {
            Section {
                Text(feedFlowStrings.settingsNotificationNoFeed)
                    .foregroundStyle(.secondary)
            }
        } else {
            Section {
                ForEach(notificationSettingState.feedSources, id: \.feedSourceId) { feedSource in
                    Toggle(isOn: Binding(
                        get: { feedSource.isEnabled },
                        set: { onFeedNotificationToggled(feedSource.feedSourceId, $0) }
                    )) {
                        Text(feedSource.feedSourceTitle)
                    }
                    .tint(.accentColor)
                }
            }
        }
    }
}
