import BackgroundTasks
import FeedFlowKit
import Foundation
import XCTest

final class BackgroundRefreshSchedulerTests: XCTestCase {
    private let now = Date(timeIntervalSince1970: 1_800_000_000)

    func testRefreshIsRequestedAsAnAppRefreshTaskOneHourOut() {
        let request: BGTaskRequest = BackgroundRefreshScheduler.makeRefreshRequest(now: now)

        XCTAssertTrue(request is BGAppRefreshTaskRequest)
        XCTAssertEqual(request.identifier, "com.prof18.feedflow.articlerefresh")
        XCTAssertEqual(request.earliestBeginDate, now.addingTimeInterval(3_600))
    }

    func testFullSyncKeepsTheProcessingTaskTwoHoursOutWithNetwork() {
        let request = BackgroundRefreshScheduler.makeFullSyncRequest(now: now)

        XCTAssertEqual(request.identifier, "com.prof18.feedflow.articlesync")
        XCTAssertEqual(request.earliestBeginDate, now.addingTimeInterval(2 * 3_600))
        XCTAssertTrue(request.requiresNetworkConnectivity)
    }

    func testFetchingAndThumbnailsLeaveTimeToPostNotificationsBeforeIOSExpiresTheRefresh() {
        // iOS expired a real app refresh 25 seconds after launching it.
        let observedExpiryMillis: Int64 = 25_000
        let worstCaseBeforePosting = BackgroundRefreshScheduler.refreshTimeBudgetMillis +
            ArticleNotificationPolicy.shared.IMAGE_BATCH_BUDGET_MS

        XCTAssertLessThanOrEqual(worstCaseBeforePosting, observedExpiryMillis - 5_000)
    }

    func testEveryRegisteredTaskIdentifierIsPermittedInTheAppInfoPlist() throws {
        let infoPlistURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("Info.plist")
        let plist = try XCTUnwrap(NSDictionary(contentsOf: infoPlistURL))
        let permitted = try XCTUnwrap(plist["BGTaskSchedulerPermittedIdentifiers"] as? [String])

        for identifier in BackgroundRefreshScheduler.taskIdentifiers {
            XCTAssertTrue(permitted.contains(identifier), "\(identifier) is not permitted")
        }
    }
}
