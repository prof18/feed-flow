import Foundation

// Calls a background task's completion exactly once. When iOS expires the task, the work is
// cancelled but may still be unwinding a database write: completing right away lets iOS suspend
// the app while it holds a lock on the app-group database (0xdead10cc). So expiry waits a short
// grace period for the work to finish on its own, and completes the task only if it doesn't.
final class BackgroundTaskCompletion: @unchecked Sendable {
    static let expiryGracePeriod: TimeInterval = 2

    private let lock = NSLock()
    private var didComplete = false
    private let complete: (Bool) -> Void

    init(complete: @escaping (Bool) -> Void) {
        self.complete = complete
    }

    func finish(success: Bool) {
        lock.lock()
        guard !didComplete else {
            lock.unlock()
            return
        }
        didComplete = true
        lock.unlock()
        complete(success)
    }

    func finishAfterGracePeriod(
        seconds: TimeInterval = BackgroundTaskCompletion.expiryGracePeriod,
        queue: DispatchQueue = .global()
    ) {
        queue.asyncAfter(deadline: .now() + seconds) { [self] in
            finish(success: false)
        }
    }
}
