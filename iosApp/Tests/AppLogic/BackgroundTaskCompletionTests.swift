import Foundation
import XCTest

final class BackgroundTaskCompletionTests: XCTestCase {
    func testCompletesOnlyOnce() {
        var results: [Bool] = []
        let completion = BackgroundTaskCompletion { results.append($0) }

        completion.finish(success: true)
        completion.finish(success: false)

        XCTAssertEqual(results, [true])
    }

    func testExpiryWaitsForTheWorkToUnwindBeforeCompleting() {
        var results: [Bool] = []
        let completion = BackgroundTaskCompletion { results.append($0) }
        let queue = DispatchQueue(label: "expiry")

        completion.finishAfterGracePeriod(seconds: 0.5, queue: queue)

        XCTAssertEqual(results, [])
        completion.finish(success: false)
        queue.sync {}
        XCTAssertEqual(results, [false])
    }

    func testExpiryCompletesTheTaskWhenTheWorkNeverUnwinds() {
        let completed = expectation(description: "completed")
        let completion = BackgroundTaskCompletion { success in
            XCTAssertFalse(success)
            completed.fulfill()
        }

        completion.finishAfterGracePeriod(seconds: 0.05, queue: .global())

        wait(for: [completed], timeout: 1)
    }
}
