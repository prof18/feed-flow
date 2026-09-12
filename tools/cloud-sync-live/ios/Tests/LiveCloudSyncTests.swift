import XCTest

final class LiveCloudSyncTests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testControlSession() throws {
        let configuration = try LiveCloudSyncConfiguration.load(from: Bundle(for: LiveCloudSyncTests.self))
        let fileStore = try LiveCloudSyncFileStore(runID: configuration.runID)
        let targetApp = XCUIApplication(bundleIdentifier: configuration.appBundleID)
        let controller = LiveCloudSyncController(
            configuration: configuration,
            fileStore: fileStore,
            targetApp: targetApp
        )
        try controller.run()
    }
}
