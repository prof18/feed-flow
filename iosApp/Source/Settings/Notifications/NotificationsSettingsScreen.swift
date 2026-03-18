import FeedFlowKit
import SwiftUI
import UIKit
import UserNotifications

struct NotificationsSettingsScreen: View {
    @Environment(AppState.self)
    private var appState

    @StateObject private var vmStoreOwner = VMStoreOwner<NotificationsViewModel>(
        Deps.shared.getNotificationsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var notificationSettingState = NotificationSettingState(
        feedSources: [],
        isEnabledForAll: false,
        notificationMode: .feedSource
    )
    @State private var permissionStatus: UNAuthorizationStatus = .notDetermined

    var body: some View {
        @Bindable var appState = appState

        NotificationsSettingsScreenContent(
            permissionStatus: permissionStatus,
            notificationSettingState: notificationSettingState,
            onRequestPermission: {
                Task {
                    await requestNotificationPermission()
                }
            },
            onOpenSettings: {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            },
            onEnableAllToggled: { enabled in
                vmStoreOwner.instance.updateAllNotificationStatus(status: enabled)
            },
            onFeedNotificationToggled: { feedSourceId, enabled in
                vmStoreOwner.instance.updateNotificationStatus(status: enabled, feedSourceId: feedSourceId)
            },
            onNotificationModeChanged: { mode in
                vmStoreOwner.instance.updateNotificationMode(mode: mode)
            }
        )
        .navigationTitle(Text(feedFlowStrings.settingsNotificationsTitle))
        .navigationBarTitleDisplayMode(.inline)
        .snackbar(messageQueue: $appState.snackbarQueue)
        .task {
            await refreshPermissionStatus()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task {
                await refreshPermissionStatus()
            }
        }
        .task {
            for await state in vmStoreOwner.instance.notificationSettingState {
                self.notificationSettingState = state
            }
        }
    }

    private func refreshPermissionStatus() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        permissionStatus = settings.authorizationStatus
    }

    private func requestNotificationPermission() async {
        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(
                options: [.alert, .badge, .sound]
            )
            permissionStatus = granted ? .authorized : .denied
        } catch {
            permissionStatus = .denied
        }
    }
}
