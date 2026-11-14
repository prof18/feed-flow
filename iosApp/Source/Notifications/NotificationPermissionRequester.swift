import UserNotifications

enum NotificationPermissionRequester {
    static func currentStatus() async -> UNAuthorizationStatus {
        await UNUserNotificationCenter.current().notificationSettings().authorizationStatus
    }

    static func requestIfNeeded() async -> UNAuthorizationStatus {
        let status = await currentStatus()
        guard status == .notDetermined else { return status }
        do {
            let granted = try await UNUserNotificationCenter.current().requestAuthorization(
                options: [.alert, .badge, .sound]
            )
            return granted ? .authorized : .denied
        } catch {
            return .denied
        }
    }
}
