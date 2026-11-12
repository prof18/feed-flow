import Foundation
import XCTest

// The test process starts no WebKit before this class runs (tests run in alphabetical class order),
// like a background launch that only fetches feeds. Reading the identifiers used to crash it.
@MainActor
final class DataStoreIdentifiersTests: XCTestCase {
    func testReadingIdentifiersAsTheFirstWebKitCallReturns() async {
        let identifiers = await WebKitWebsiteDataStoreProvider().identifiers()

        XCTAssertFalse(identifiers.contains(UUID()))
    }
}
