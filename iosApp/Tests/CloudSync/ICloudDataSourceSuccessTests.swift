import FeedFlowKit
import Foundation
import XCTest

final class ICloudDataSourceSuccessTests: XCTestCase {
    func testCoordinatedUploadAndDownloadUseLocalFiles() async throws {
        let root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        let cloud = root.appendingPathComponent("cloud/Documents", isDirectory: true)
        let staging = root.appendingPathComponent("staging", isDirectory: true)
        try FileManager.default.createDirectory(at: staging, withIntermediateDirectories: true)
        defer { try? FileManager.default.removeItem(at: root) }

        let source = root.appendingPathComponent("source.db")
        let name = "backup.db"
        let dataSource = ICloudDataSourceImpl(
            logger: KermitLogger.companion.withTag(tag: "ICloudTests"),
            localBaseFolderURL: cloud,
            localTemporaryFolderURL: staging,
            fileCoordinator: FoundationICloudFileCoordinator(),
            fileDiscovery: LocalICloudFileDiscovery()
        )
        let bytes = Data("initial snapshot".utf8)
        try bytes.write(to: source)
        let initialUpload = try await dataSource.performUpload(databasePath: source, databaseName: name)
        XCTAssertTrue(initialUpload is ICloudUploadResult.Success, "\(initialUpload)")
        XCTAssertEqual(try Data(contentsOf: cloud.appendingPathComponent(name)), bytes)

        let updated = Data("updated snapshot".utf8)
        try updated.write(to: source)
        let update = try await dataSource.performUpload(databasePath: source, databaseName: name)
        XCTAssertTrue(update is ICloudUploadResult.Success, "\(update)")
        let download = try await dataSource.performDownload(databaseName: name)
        XCTAssertTrue(download is ICloudDownloadResult.Success, "\(download)")
        XCTAssertEqual(try Data(contentsOf: staging.appendingPathComponent(name)), updated)

        try FileManager.default.removeItem(at: source)
        let failedUpload = try await dataSource.performUpload(databasePath: source, databaseName: name)
        XCTAssertTrue(failedUpload is ICloudUploadResult.Error)
        XCTAssertEqual(try Data(contentsOf: cloud.appendingPathComponent(name)), updated)
    }
}
