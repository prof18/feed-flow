import FeedFlowKit

actor WidgetRefreshCoordinator {
    static let shared = WidgetRefreshCoordinator()
    private var refreshTask: Task<Void, Never>?

    func refresh() async throws {
        if let refreshTask {
            try await wait(for: refreshTask)
            return
        }
        let task = Task<Void, Never> {
            WidgetKoin.ensureStarted()
            // Each successfully fetched feed is already persisted if a later fetch fails.
            try? await Deps.shared.getSerialFeedFetcherRepository().fetchFeeds(forceRefresh: true)
        }
        refreshTask = task
        defer { refreshTask = nil }
        try await wait(for: task)
    }

    private func wait(for task: Task<Void, Never>) async throws {
        await withTaskCancellationHandler {
            await task.value
        } onCancel: {
            task.cancel()
        }
        try Task.checkCancellation()
    }
}
