import FeedFlowKit
import Foundation
import XCTest

final class DropboxDataSourceIosSuccessTests: XCTestCase {
    func testResumedUploadIsAcknowledgedOnlyForAValidSuccessfulResponse() {
        XCTAssertTrue(
            DropboxDataSourceIos.shouldAcknowledgeResumedUpload(
                response: NSObject(),
                error: nil
            )
        )
        XCTAssertFalse(
            DropboxDataSourceIos.shouldAcknowledgeResumedUpload(
                response: NSObject(),
                error: NSError(domain: "DropboxResumeTests", code: 1)
            )
        )
        XCTAssertFalse(
            DropboxDataSourceIos.shouldAcknowledgeResumedUpload(
                response: NSObject?.none,
                error: nil
            )
        )
    }

    func testSuccessfulUploadAndDownloadPreserveBytes() throws {
        let fixtureRoot = try makeFixtureRoot()
        defer { try? FileManager.default.removeItem(at: fixtureRoot) }

        let sourceURL = fixtureRoot.appendingPathComponent("source.sqlite")
        let expectedBytes = Data("dropbox-success-fixture".utf8)
        try expectedBytes.write(to: sourceURL)

        let client = FakeDropboxClient()
        let dataSource = DropboxDataSourceIos(
            client: client,
            documentsDirectoryURL: { fixtureRoot }
        )

        let uploadExpectation = expectation(description: "Dropbox upload completes")
        var uploadResult: DropboxUploadResult?
        var uploadError: Error?
        dataSource.performUpload(
            uploadParam: DropboxUploadParam(
                path: "/FeedFlow.sqlite",
                url: sourceURL,
                expectedRevision: "expected-revision"
            )
        ) { result, error in
            uploadResult = result
            uploadError = error
            uploadExpectation.fulfill()
        }
        wait(for: [uploadExpectation], timeout: 1)

        XCTAssertNil(uploadError)
        assertSuccessfulUploadResult(uploadResult, byteCount: expectedBytes.count)
        XCTAssertEqual(client.uploadedExpectedRevision, "expected-revision")
        XCTAssertEqual(client.remoteFiles["/FeedFlow.sqlite"], expectedBytes)

        let downloadExpectation = expectation(description: "Dropbox download completes")
        var downloadResult: DropboxDownloadResult?
        var downloadError: Error?
        dataSource.performDownload(
            downloadParam: DropboxDownloadParam(outputName: "restored.sqlite", path: "/FeedFlow.sqlite")
        ) { result, error in
            downloadResult = result
            downloadError = error
            downloadExpectation.fulfill()
        }
        wait(for: [downloadExpectation], timeout: 1)

        XCTAssertNil(downloadError)
        XCTAssertEqual(downloadResult?.id, "dropbox-file-id")
        XCTAssertEqual(downloadResult?.sizeInByte, Int64(expectedBytes.count))
        XCTAssertEqual(downloadResult?.contentHash, "downloaded-hash")
        XCTAssertEqual(downloadResult?.revision, "downloaded-revision")
        XCTAssertEqual(
            try Data(contentsOf: fixtureRoot.appendingPathComponent("restored.sqlite")),
            expectedBytes
        )
    }

    func testUploadConflictReturnsFlaggedResult() throws {
        let fixtureRoot = try makeFixtureRoot()
        defer { try? FileManager.default.removeItem(at: fixtureRoot) }
        let sourceURL = fixtureRoot.appendingPathComponent("source.sqlite")
        try Data("conflicting-data".utf8).write(to: sourceURL)
        let client = ConflictingDropboxClient()
        let dataSource = DropboxDataSourceIos(
            client: client,
            documentsDirectoryURL: { fixtureRoot },
            logError: { _ in }
        )
        let uploadExpectation = expectation(description: "Dropbox conflict completes")

        dataSource.performUpload(
            uploadParam: DropboxUploadParam(path: "/FeedFlow.sqlite", url: sourceURL, expectedRevision: nil)
        ) { result, error in
            XCTAssertNil(error)
            XCTAssertEqual(result?.isConflict, true)
            XCTAssertNil(result?.revision)
            XCTAssertNil(client.uploadedExpectedRevision)
            uploadExpectation.fulfill()
        }
        wait(for: [uploadExpectation], timeout: 1)
    }

    func testConfirmedMissingDownloadReturnsExplicitNotFoundResult() throws {
        let fixtureRoot = try makeFixtureRoot()
        defer { try? FileManager.default.removeItem(at: fixtureRoot) }
        let dataSource = DropboxDataSourceIos(
            client: MissingDropboxClient(),
            documentsDirectoryURL: { fixtureRoot },
            logError: { _ in }
        )
        let downloadExpectation = expectation(description: "Dropbox download fails")
        var downloadError: Error?

        dataSource.performDownload(
            downloadParam: DropboxDownloadParam(outputName: "restored.sqlite", path: "/missing.sqlite")
        ) { result, error in
            XCTAssertEqual(result?.isBackupNotFound, true)
            XCTAssertNil(result?.destinationUrl)
            downloadError = error
            downloadExpectation.fulfill()
        }
        wait(for: [downloadExpectation], timeout: 1)

        XCTAssertNil(downloadError)
    }

    private func makeFixtureRoot() throws -> URL {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent("FeedFlowDropboxTests", isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
        return root
    }

    private func assertSuccessfulUploadResult(_ result: DropboxUploadResult?, byteCount: Int) {
        XCTAssertEqual(result?.id, "dropbox-file-id")
        XCTAssertEqual(result?.editDateMillis, 1_700_000_000_000)
        XCTAssertEqual(result?.sizeInByte, Int64(byteCount))
        XCTAssertEqual(result?.contentHash, "uploaded-hash")
        XCTAssertEqual(result?.revision, "uploaded-revision")
        XCTAssertEqual(result?.isConflict, false)
    }
}

private final class MissingDropboxClient: DropboxClientBridge {
    func upload(
        path _: String,
        expectedRevision _: String?,
        input _: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    ) {
        completion(nil, NSError(domain: "MissingDropboxClient", code: 1))
    }

    func download(
        path _: String,
        overwrite _: Bool,
        destination _: URL,
        completion: @escaping (DropboxDownloadResponse?, Error?) -> Void
    ) {
        completion(nil, DropboxErrors.downloadNotFound)
    }
}

private final class FakeDropboxClient: DropboxClientBridge {
    var remoteFiles: [String: Data] = [:]
    private(set) var uploadedExpectedRevision: String?

    func upload(
        path: String,
        expectedRevision: String?,
        input: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    ) {
        do {
            uploadedExpectedRevision = expectedRevision
            let data = try Data(contentsOf: input)
            remoteFiles[path] = data
            completion(
                DropboxFileMetadata(
                    id: "dropbox-file-id",
                    size: UInt64(data.count),
                    serverModified: Date(timeIntervalSince1970: 1_700_000_000),
                    contentHash: "uploaded-hash",
                    revision: "uploaded-revision"
                ),
                nil
            )
        } catch {
            completion(nil, error)
        }
    }

    func download(
        path: String,
        overwrite: Bool,
        destination: URL,
        completion: @escaping (DropboxDownloadResponse?, Error?) -> Void
    ) {
        guard let data = remoteFiles[path] else {
            completion(nil, NSError(domain: "FakeDropboxClient", code: 1))
            return
        }

        do {
            if !overwrite, FileManager.default.fileExists(atPath: destination.path) {
                throw NSError(domain: "FakeDropboxClient", code: 2)
            }
            try data.write(to: destination)
            completion(
                DropboxDownloadResponse(
                    metadata: DropboxFileMetadata(
                        id: "dropbox-file-id",
                        size: UInt64(data.count),
                        serverModified: Date(timeIntervalSince1970: 1_700_000_000),
                        contentHash: "downloaded-hash",
                        revision: "downloaded-revision"
                    ),
                    destination: destination
                ),
                nil
            )
        } catch {
            completion(nil, error)
        }
    }
}

private final class ConflictingDropboxClient: DropboxClientBridge {
    private(set) var uploadedExpectedRevision: String?

    func upload(
        path _: String,
        expectedRevision: String?,
        input _: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    ) {
        uploadedExpectedRevision = expectedRevision
        completion(nil, DropboxErrors.uploadConflict)
    }

    func download(
        path _: String,
        overwrite _: Bool,
        destination _: URL,
        completion: @escaping (DropboxDownloadResponse?, Error?) -> Void
    ) {
        completion(nil, NSError(domain: "ConflictingDropboxClient", code: 1))
    }
}
