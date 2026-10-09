import XCTest

final class WidgetRefreshCoordinatorTests: XCTestCase {
    func testRefreshPropagatesFailureAndAllowsRetry() async throws {
        let coordinator = WidgetRefreshCoordinator()
        do {
            try await coordinator.refresh {
                throw NSError(domain: "WidgetRefreshTests", code: 1)
            }
            XCTFail("Refresh should propagate the fetch failure")
        } catch {
            XCTAssertEqual((error as NSError).domain, "WidgetRefreshTests")
        }

        let retried = expectation(description: "A failed refresh can be retried")
        try await coordinator.refresh { retried.fulfill() }
        await fulfillment(of: [retried], timeout: 1)
    }

    func testCancellationReachesFetchAndAllowsRetry() async throws {
        let coordinator = WidgetRefreshCoordinator()
        let started = expectation(description: "Fetch started")
        let cancelled = expectation(description: "Fetch cancelled")
        let caller = Task {
            try await coordinator.refresh {
                started.fulfill()
                do {
                    try await Task.sleep(for: .seconds(60))
                } catch {
                    cancelled.fulfill()
                    throw error
                }
            }
        }
        await fulfillment(of: [started], timeout: 1)

        caller.cancel()
        do {
            try await caller.value
            XCTFail("Refresh should propagate cancellation")
        } catch {
            XCTAssertTrue(error is CancellationError)
        }
        await fulfillment(of: [cancelled], timeout: 1)

        let retried = expectation(description: "A cancelled refresh can be retried")
        try await coordinator.refresh { retried.fulfill() }
        await fulfillment(of: [retried], timeout: 1)
    }
}
