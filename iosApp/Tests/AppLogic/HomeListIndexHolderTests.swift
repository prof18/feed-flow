import FeedFlowKit
import Foundation
import XCTest

final class HomeListIndexHolderTests: XCTestCase {
    func testReportsOnlyTheRowsCurrentlyOnScreen() {
        let holder = HomeListIndexHolder(fakeHomeViewModel: true)

        holder.visibleItemsChanged([
            VisibleFeedItem(id: "a", index: 0),
            VisibleFeedItem(id: "b", index: 1),
        ])

        XCTAssertTrue(holder.isVisible(id: "b"))
        XCTAssertFalse(holder.isVisible(id: "z"))
    }

    func testClearingForgetsTheVisibleRows() {
        let holder = HomeListIndexHolder(fakeHomeViewModel: true)
        holder.visibleItemsChanged([VisibleFeedItem(id: "a", index: 0)])

        holder.clear()

        XCTAssertFalse(holder.isVisible(id: "a"))
    }
}
