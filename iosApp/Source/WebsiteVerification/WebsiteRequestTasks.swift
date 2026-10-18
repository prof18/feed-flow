import Foundation

final class WebsiteRequestTasks {
    private let lock = NSLock()
    private var tasks: [String: Task<Void, Never>] = [:]

    func start(id: String, operation: @escaping () async -> Void) {
        lock.lock()
        tasks[id] = Task {
            await operation()
            self.finish(id: id)
        }
        lock.unlock()
    }

    func cancel(id: String) {
        lock.lock()
        let task = tasks.removeValue(forKey: id)
        lock.unlock()
        task?.cancel()
    }

    private func finish(id: String) {
        lock.lock()
        tasks.removeValue(forKey: id)
        lock.unlock()
    }
}
