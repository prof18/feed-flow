import FeedFlowKit
import Foundation
import UserNotifications

/// Removes delivered new-article notifications once everything behind them has been read, here or
/// on another device whose read state syncs in.
///
/// It watches the shared change signal, which the live-query gate pauses while the app is in the
/// background, so it only works while FeedFlow is active and catches up when it is opened again.
@MainActor
final class ReadNotificationWatcher {
    private let delivered: () async -> [DeliveredNotification]
    private let staleTags: ([String]) async throws -> [String]
    private let remove: ([String]) -> Void
    private var task: Task<Void, Never>?

    init(
        delivered: @escaping () async -> [DeliveredNotification] = {
            await UNUserNotificationCenter.current().deliveredNotifications().compactMap { DeliveredNotification($0) }
        },
        staleTags: @escaping ([String]) async throws -> [String] = { tags in
            try await Deps.shared.getReadNotificationResolver().staleTags(tags: tags)
        },
        remove: @escaping ([String]) -> Void = { identifiers in
            UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: identifiers)
        }
    ) {
        self.delivered = delivered
        self.staleTags = staleTags
        self.remove = remove
    }

    func start() {
        guard task == nil else { return }
        let changes = Deps.shared.getReadNotificationResolver().changes()
        task = Task { [weak self] in
            for await _ in changes {
                guard let self else { return }
                await self.dismissReadNotifications()
            }
        }
    }

    func dismissReadNotifications() async {
        let posted = await delivered()
        guard !posted.isEmpty else { return }
        let stale: Set<String>
        do {
            stale = Set(try await staleTags(posted.map(\.tag)))
        } catch {
            // Skip this check; the next change tries again.
            return
        }
        let identifiers = posted.filter { stale.contains($0.tag) }.map(\.identifier)
        guard !identifiers.isEmpty else { return }
        remove(identifiers)
    }
}
