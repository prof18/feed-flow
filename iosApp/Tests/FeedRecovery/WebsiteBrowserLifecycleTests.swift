import Foundation
import XCTest

final class WebsiteBrowserLifecycleTests: XCTestCase {
    func testSameRequestUpdateDoesNotRearmCompletedCapture() {
        var outcome = WebsiteBrowserCaptureOutcome()
        XCTAssertTrue(outcome.beginRequest(id: "request-1"))
        XCTAssertTrue(outcome.complete(requestID: "request-1"))

        XCTAssertFalse(outcome.beginRequest(id: "request-1"))
        XCTAssertTrue(outcome.isComplete)
    }

    func testNextSameURLWaiterCanCaptureOnTheReadyPage() {
        let navigation = NSObject()
        var navigationState = WebsiteBrowserNavigationState()
        navigationState.started(navigation)
        XCTAssertTrue(navigationState.finished(navigation))

        var outcome = WebsiteBrowserCaptureOutcome()
        XCTAssertTrue(outcome.beginRequest(id: "request-1"))
        XCTAssertTrue(outcome.complete(requestID: "request-1"))
        XCTAssertTrue(outcome.beginRequest(id: "request-2"))

        XCTAssertFalse(outcome.isComplete)
        XCTAssertTrue(navigationState.canCapture(generation: navigationState.generation))
    }

    func testCompletionFromOldRequestCannotCompleteCurrentWaiter() {
        var outcome = WebsiteBrowserCaptureOutcome()
        XCTAssertTrue(outcome.beginRequest(id: "request-1"))
        XCTAssertTrue(outcome.beginRequest(id: "request-2"))

        XCTAssertFalse(outcome.complete(requestID: "request-1"))
        XCTAssertFalse(outcome.isComplete)
    }

    @MainActor
    func testCloseDuringProfileAttachPreventsInstallingTheAttachedWebView() async {
        let lifecycle = WebsiteBrowserAttachLifecycle()
        var resumeAttach: CheckedContinuation<Void, Never>?
        let attach = Task {
            await withCheckedContinuation { continuation in
                resumeAttach = continuation
            }
            return lifecycle.canContinue(taskIsCancelled: Task.isCancelled)
        }

        while resumeAttach == nil { await Task.yield() }
        lifecycle.close()
        resumeAttach?.resume()

        let shouldInstall = await attach.value
        XCTAssertFalse(shouldInstall)
    }

    @MainActor
    func testCloseDuringPreviousReadDrainPreventsQueuedNavigation() async {
        let lifecycle = WebsiteBrowserAttachLifecycle()
        var resumeRead: CheckedContinuation<Void, Never>?
        let previousRead = Task {
            await withCheckedContinuation { continuation in
                resumeRead = continuation
            }
        }
        var loadedURLs: [String] = []
        let queuedRequest = Task {
            await previousRead.value
            guard lifecycle.canContinue(taskIsCancelled: Task.isCancelled) else { return }
            loadedURLs.append("https://fixture.test/queued-feed.xml")
        }

        while resumeRead == nil { await Task.yield() }
        lifecycle.close()
        queuedRequest.cancel()
        previousRead.cancel()
        resumeRead?.resume()
        await queuedRequest.value

        XCTAssertTrue(loadedURLs.isEmpty)
    }

    @MainActor
    func testCancelledRequestCannotNavigateAfterPreviousReadDrains() async {
        let lifecycle = WebsiteBrowserAttachLifecycle()
        var resumeRead: CheckedContinuation<Void, Never>?
        let previousRead = Task {
            await withCheckedContinuation { continuation in
                resumeRead = continuation
            }
        }
        let queuedRequest = Task {
            await previousRead.value
            return lifecycle.canContinue(taskIsCancelled: Task.isCancelled)
        }

        while resumeRead == nil { await Task.yield() }
        queuedRequest.cancel()
        resumeRead?.resume()

        let shouldNavigate = await queuedRequest.value
        XCTAssertFalse(shouldNavigate)
    }
}
