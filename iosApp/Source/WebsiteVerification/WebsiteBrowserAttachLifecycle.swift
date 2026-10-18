@MainActor
final class WebsiteBrowserAttachLifecycle {
    private(set) var isClosed = false

    func close() {
        isClosed = true
    }

    func canContinue(taskIsCancelled: Bool) -> Bool {
        !isClosed && !taskIsCancelled
    }
}
