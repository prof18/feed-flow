import Foundation
import Observation

@MainActor
@Observable
final class NotificationDeepLinkStore {
    static let shared = NotificationDeepLinkStore()

    private(set) var pendingURL: URL?

    func enqueue(_ url: URL) {
        let components = url.pathComponents.filter { $0 != "/" }
        guard url.scheme == "feedflow",
              let host = url.host, ["feed", "feedsourcefilter", "category"].contains(host),
              components.count == 1, let id = components.first,
              !id.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        pendingURL = url
    }

    func consume() -> URL? {
        guard let url = pendingURL else { return nil }
        pendingURL = nil
        NotificationCenter.default.post(name: .didReceiveNotificationDeepLink, object: nil)
        return url
    }
}
