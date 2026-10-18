import Foundation
import XCTest

final class WebsiteBrowserNavigationStateTests: XCTestCase {
    func testChallengeReloadDiscardsOldCaptureAndAllowsNewCapture() {
        let challenge = NSObject()
        let article = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(challenge)
        XCTAssertTrue(state.finished(challenge))
        let interruptedCapture = state.generation
        state.started(article)
        XCTAssertFalse(state.canCapture(generation: interruptedCapture))
        XCTAssertTrue(state.finished(article))
        XCTAssertFalse(state.canCapture(generation: interruptedCapture))
        XCTAssertTrue(state.canCapture(generation: state.generation))
    }

    func testLateFinishCannotCaptureThePageDuringItsReplacementLoad() {
        let old = NSObject()
        let current = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(old)
        state.started(current)
        XCTAssertFalse(state.finished(old))
        XCTAssertFalse(state.ready)
        XCTAssertTrue(state.finished(current))
    }

    func testLateFailureCannotStopReplacementNavigation() {
        let old = NSObject()
        let current = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(old)
        state.started(current)
        XCTAssertTrue(state.finished(current))
        XCTAssertFalse(state.failed(old, error: URLError(.networkConnectionLost)))
        XCTAssertTrue(state.ready)
    }

    func testCancelledNavigationDoesNotStopUsablePage() {
        assertNonfatal(NSError(domain: NSURLErrorDomain, code: NSURLErrorCancelled))
    }

    func testPolicyInterruptionDoesNotStopUsableChallengePage() {
        assertNonfatal(NSError(domain: "WebKitErrorDomain", code: 102))
    }

    func testRSSPolicyInterruptionAllowsCaptureWithoutDidFinish() {
        let navigation = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(navigation)
        XCTAssertFalse(state.failed(navigation, error: NSError(domain: "WebKitErrorDomain", code: 102)))
        XCTAssertTrue(state.canCapture(generation: state.generation))
    }

    func testNetworkFailureStopsCurrentNavigation() {
        let navigation = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(navigation)
        XCTAssertTrue(state.finished(navigation))
        XCTAssertTrue(state.failed(navigation, error: URLError(.notConnectedToInternet)))
        XCTAssertFalse(state.ready)
    }

    func testCancellationCodeInDifferentDomainIsStillFatal() {
        let navigation = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(navigation)
        XCTAssertTrue(state.failed(navigation, error: NSError(domain: "OtherError", code: NSURLErrorCancelled)))
    }

    private func assertNonfatal(_ error: Error, file: StaticString = #filePath, line: UInt = #line) {
        let navigation = NSObject()
        var state = WebsiteBrowserNavigationState()
        state.started(navigation)
        XCTAssertTrue(state.finished(navigation), file: file, line: line)
        XCTAssertFalse(state.failed(navigation, error: error), file: file, line: line)
        XCTAssertTrue(state.ready, file: file, line: line)
    }
}
