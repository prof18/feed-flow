actor WidgetRefreshCoordinator {
    static let shared = WidgetRefreshCoordinator()
    private var refreshTask: Task<Void, Error>?

    func refresh(operation: @escaping @Sendable () async throws -> Void) async throws {
        if let refreshTask {
            try await wait(for: refreshTask)
            return
        }
        let task = Task<Void, Error> {
            try await operation()
        }
        refreshTask = task
        defer { refreshTask = nil }
        try await wait(for: task)
    }

    private func wait(for task: Task<Void, Error>) async throws {
        try await withTaskCancellationHandler {
            try await task.value
        } onCancel: {
            task.cancel()
        }
        try Task.checkCancellation()
    }
}
