import FeedFlowKit
import SwiftUI
import UIKit
import UserNotifications

struct NotificationsSettingsScreen: View {
    @StateObject private var vmStoreOwner = VMStoreOwner<NotificationsViewModel>(
        Deps.shared.getNotificationsViewModel()
    )
    private let feedFlowStrings = Deps.shared.getStrings()

    @State private var notificationSettingState = NotificationSettingState(
        feedSources: [],
        feedGroups: [],
        searchQuery: "",
        isEnabledForAll: false,
        notificationMode: .feedSource,
        isLoading: true
    )
    @State private var permissionStatus: UNAuthorizationStatus?
    @State private var backgroundRefreshStatus: UIBackgroundRefreshStatus = UIApplication.shared.backgroundRefreshStatus

    var body: some View {
        NotificationsSettingsScreenContent(
            permissionStatus: permissionStatus,
            backgroundRefreshStatus: backgroundRefreshStatus,
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
            },
            onSearchQueryChanged: { query in
                vmStoreOwner.instance.updateSearchQuery(query: query)
            }
        )
        .navigationTitle(Text(feedFlowStrings.settingsNotificationsTitle))
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await refreshPermissionStatus()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            Task {
                await refreshPermissionStatus()
            }
        }
        .onReceive(
            NotificationCenter.default.publisher(for: UIApplication.backgroundRefreshStatusDidChangeNotification)
        ) { _ in
            backgroundRefreshStatus = UIApplication.shared.backgroundRefreshStatus
        }
        .task {
            for await state in vmStoreOwner.instance.notificationSettingState {
                self.notificationSettingState = state
            }
        }
    }

    private func refreshPermissionStatus() async {
        permissionStatus = await NotificationPermissionRequester.currentStatus()
        backgroundRefreshStatus = UIApplication.shared.backgroundRefreshStatus
    }

    private func requestNotificationPermission() async {
        permissionStatus = await NotificationPermissionRequester.requestIfNeeded()
    }
}
