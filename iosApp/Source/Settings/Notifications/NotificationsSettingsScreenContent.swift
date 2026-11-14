import FeedFlowKit
import SwiftUI
import UIKit
import UserNotifications

struct NotificationsSettingsScreenContent: View {
    let permissionStatus: UNAuthorizationStatus?
    let backgroundRefreshStatus: UIBackgroundRefreshStatus
    let notificationSettingState: NotificationSettingState
    let onRequestPermission: () -> Void
    let onOpenSettings: () -> Void
    let onEnableAllToggled: (Bool) -> Void
    let onFeedNotificationToggled: (String, Bool) -> Void
    let onNotificationModeChanged: (NotificationMode) -> Void
    let onSearchQueryChanged: (String) -> Void

    private let feedFlowStrings = Deps.shared.getStrings()

    var body: some View {
        if notificationSettingState.feedSources.isEmpty {
            form
        } else {
            form
                .searchable(
                    text: Binding(
                        get: { notificationSettingState.searchQuery },
                        set: { onSearchQueryChanged($0) }
                    ),
                    prompt: Text(feedFlowStrings.settingsNotificationsSearchPrompt)
                )
        }
    }

    private var form: some View {
        Form {
            if let permissionStatus {
                if permissionStatus != .authorized && permissionStatus != .provisional {
                    permissionSection(status: permissionStatus)
                }
                if backgroundRefreshStatus != .available {
                    backgroundAppRefreshSection
                }
                warningSection
                notificationModeSection
                feedsSection
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
            }
        }
        .scrollContentBackground(.hidden)
        .background(Color.secondaryBackgroundColor)
    }

    private func permissionSection(status: UNAuthorizationStatus) -> some View {
        Section {
            Text(feedFlowStrings.settingsNotificationsPermissionDescription)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier(NotificationsAccessibilityIdentifiers.permissionSection)

            switch status {
            case .denied:
                Button {
                    onOpenSettings()
                } label: {
                    Label(feedFlowStrings.settingsNotificationsOpenSettingsButton, systemImage: "gear")
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

    private var backgroundAppRefreshSection: some View {
        Section {
            Text(feedFlowStrings.settingsNotificationsBackgroundAppRefreshOff)
                .foregroundStyle(.secondary)
                .accessibilityIdentifier(NotificationsAccessibilityIdentifiers.backgroundAppRefreshOff)

            Button {
                onOpenSettings()
            } label: {
                Label(feedFlowStrings.settingsNotificationsOpenSettingsButton, systemImage: "gear")
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
                Text(feedFlowStrings.settingsNotificationModeArticle)
                    .tag(NotificationMode.article)
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
            if notificationSettingState.notificationMode == .article {
                Text(feedFlowStrings.settingsNotificationModeArticleDesc)
            } else {
                Text(feedFlowStrings.settingsNotificationModeDesc)
            }
        }
    }

    @ViewBuilder private var feedsSection: some View {
        if notificationSettingState.feedSources.isEmpty {
            Section {
                Text(feedFlowStrings.settingsNotificationNoFeed)
                    .foregroundStyle(.secondary)
            }
        } else if notificationSettingState.feedGroups.isEmpty && !notificationSettingState.searchQuery.isEmpty {
            Section {
                Text(feedFlowStrings.searchNoData(notificationSettingState.searchQuery))
                    .foregroundStyle(.secondary)
            }
        } else {
            ForEach(notificationSettingState.feedGroups, id: \.categoryTitle) { group in
                Section {
                    ForEach(group.feedSources, id: \.feedSourceId) { feedSource in
                        Toggle(isOn: Binding(
                            get: { feedSource.isEnabled },
                            set: { onFeedNotificationToggled(feedSource.feedSourceId, $0) }
                        )) {
                            Text(feedSource.feedSourceTitle)
                        }
                        .tint(.accentColor)
                    }
                } header: {
                    Text(group.categoryTitle ?? feedFlowStrings.noCategory)
                }
            }
        }
    }
}
