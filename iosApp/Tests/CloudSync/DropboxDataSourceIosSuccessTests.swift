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
            uploadParam: DropboxUploadParam(path: "/FeedFlow.sqlite", url: sourceURL)
        ) { result, error in
            uploadResult = result
            uploadError = error
            uploadExpectation.fulfill()
        }
        wait(for: [uploadExpectation], timeout: 1)

        XCTAssertNil(uploadError)
        XCTAssertNotNil(uploadResult)
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
        XCTAssertNotNil(downloadResult)
        XCTAssertEqual(
            try Data(contentsOf: fixtureRoot.appendingPathComponent("restored.sqlite")),
            expectedBytes
        )
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
}

private final class MissingDropboxClient: DropboxClientBridge {
    func upload(
        path _: String,
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

    func upload(
        path: String,
        input: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    ) {
        do {
            let data = try Data(contentsOf: input)
            remoteFiles[path] = data
            completion(
                DropboxFileMetadata(
                    id: "dropbox-file-id",
                    size: UInt64(data.count),
                    serverModified: Date(timeIntervalSince1970: 1_700_000_000),
                    contentHash: nil
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
                        contentHash: nil
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
