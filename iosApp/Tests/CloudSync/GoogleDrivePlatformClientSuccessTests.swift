import FeedFlowKit
import Foundation
import GoogleAPIClientForREST_Drive
import XCTest

final class GoogleDrivePlatformClientSuccessTests: XCTestCase {
    func testSuccessfulUploadAndDownloadPreserveBytes() {
        let expectedBytes = Data("drive-success-fixture".utf8)
        let service = FakeGoogleDriveServiceClient()
        let client = GoogleDrivePlatformClient(service: service)

        let uploadExpectation = expectation(description: "Drive upload completes")
        var uploadedFileId: String?
        var uploadError: KotlinThrowable?
        client.uploadFile(
            data: expectedBytes,
            fileName: "FeedFlow.sqlite",
            existingFileId: nil
        ) { fileId, error in
            uploadedFileId = fileId
            uploadError = error
            uploadExpectation.fulfill()
        }
        wait(for: [uploadExpectation], timeout: 1)

        XCTAssertNil(uploadError)
        XCTAssertEqual(uploadedFileId, "drive-file-id")
        XCTAssertEqual(service.remoteFiles["FeedFlow.sqlite"]?.data, expectedBytes)

        let downloadExpectation = expectation(description: "Drive download completes")
        client.downloadFile(
            fileName: "FeedFlow.sqlite",
            existingFileId: nil
        ) { data, fileId, error in
            XCTAssertNil(error)
            XCTAssertEqual(data, expectedBytes)
            XCTAssertEqual(fileId, "drive-file-id")
            downloadExpectation.fulfill()
        }
        wait(for: [downloadExpectation], timeout: 1)
    }

    func testSearchTransportErrorDoesNotCreateFile() {
        let service = ScriptedGoogleDriveServiceClient(steps: [.failure(.search, code: 503)])
        let result = upload(using: service, existingFileId: nil)

        XCTAssertNil(result.fileId)
        XCTAssertNotNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search])
    }

    func testCachedFileUpdateTransportErrorDoesNotCreateFile() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["cached-id"]),
            .failure(.update, code: 503),
        ])
        let result = upload(using: service, existingFileId: "stale-id")

        XCTAssertNil(result.fileId)
        XCTAssertNotNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update])
    }

    func testCachedFileNotFoundRediscoversBeforeUpdating() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["stale-id"]),
            .failure(.update, code: 404),
            .files(["current-id"]),
            .file(.update, id: "current-id"),
        ])
        let result = upload(using: service, existingFileId: "stale-id")

        XCTAssertEqual(result.fileId, "current-id")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search, .update])
    }

    func testCachedFileNotFoundCreatesOnlyAfterSuccessfulEmptyDiscovery() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["stale-id"]),
            .failure(.update, code: 404),
            .files([]),
            .file(.create, id: "new-id"),
        ])
        let result = upload(using: service, existingFileId: "stale-id")

        XCTAssertEqual(result.fileId, "new-id")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search, .create])
    }

    func testCachedFileNotFoundDoesNotSucceedWhenRediscoveredIdIsAlsoMissing() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["stale-id"]),
            .failure(.update, code: 404),
            .files(["other-id"]),
            .failure(.update, code: 404),
        ])
        let result = upload(using: service, existingFileId: "stale-id")

        XCTAssertNil(result.fileId)
        XCTAssertNotNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search, .update])
    }

    func testDuplicateFilesUploadUpdatesNewestAndRenamesOthers() {
        let service = ScriptedGoogleDriveServiceClient(steps: duplicateUploadSteps())
        let result = upload(using: service, existingFileId: nil)

        XCTAssertEqual(result.fileId, "new")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search, .update])
        assertTransferredNewestAndRenamedOld(service)
    }

    func testCachedIdIsIgnoredInFavourOfNewest() {
        let service = ScriptedGoogleDriveServiceClient(steps: duplicateUploadSteps())
        let result = upload(using: service, existingFileId: "old")

        XCTAssertEqual(result.fileId, "new")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search, .update])
        assertTransferredNewestAndRenamedOld(service)
    }

    func testRenameFailureDoesNotFailUpload() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .datedFiles([Self.old, Self.new]),
            .file(.update, id: "new"),
            .datedFiles([Self.old, Self.new]),
            .failure(.update, code: 500),
        ])
        let result = upload(using: service, existingFileId: nil)

        XCTAssertEqual(result.fileId, "new")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.transfers, ["new"])
    }

    func testRenameSkippedWhenNewestChanged() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .datedFiles([Self.old, Self.new]),
            .file(.update, id: "new"),
            .datedFiles([Self.newerOld, Self.new]),
        ])
        let result = upload(using: service, existingFileId: nil)

        XCTAssertEqual(result.fileId, "new")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .update, .search])
        XCTAssertEqual(service.transfers, ["new"])
        XCTAssertTrue(service.renames.isEmpty)
    }

    func testCachedDownloadNotFoundRediscoversUniqueFile() {
        let expectedData = Data("rediscovered-drive-data".utf8)
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["stale-id"]),
            .failure(.download, code: 404),
            .files(["current-id"]),
            .data(expectedData),
        ])
        let client = GoogleDrivePlatformClient(service: service)
        let completion = expectation(description: "Drive download completes")
        var actualData: Data?
        var actualError: KotlinThrowable?

        client.downloadFile(fileName: "FeedFlow.sqlite", existingFileId: "stale-id") { data, fileId, error in
            XCTAssertEqual(fileId, "current-id")
            actualData = data
            actualError = error
            completion.fulfill()
        }
        wait(for: [completion], timeout: 1)

        XCTAssertEqual(actualData, expectedData)
        XCTAssertNil(actualError)
        XCTAssertEqual(service.executedOperations, [.search, .download, .search, .download])
    }

    func testDuplicateFilesDownloadFetchesNewestAndRenamesOthers() {
        let expectedData = Data("newest-drive-data".utf8)
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .datedFiles([Self.old, Self.new]),
            .data(expectedData),
            .datedFiles([Self.old, Self.new]),
            .file(.update, id: "old"),
        ])
        let client = GoogleDrivePlatformClient(service: service)
        let completion = expectation(description: "Drive download completes")
        var actualData: Data?
        var actualFileId: String?
        var actualError: KotlinThrowable?

        client.downloadFile(fileName: "FeedFlow.sqlite", existingFileId: nil) { data, fileId, error in
            actualData = data
            actualFileId = fileId
            actualError = error
            completion.fulfill()
        }
        wait(for: [completion], timeout: 1)

        XCTAssertEqual(actualData, expectedData)
        XCTAssertEqual(actualFileId, "new")
        XCTAssertNil(actualError)
        XCTAssertEqual(service.executedOperations, [.search, .download, .search, .update])
        assertTransferredNewestAndRenamedOld(service)
    }

    func testDuplicatesAcrossPagesPickNewestFromAllPages() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .datedPage([Self.old], nextPageToken: "page-2"),
            .datedFiles([Self.new]),
            .file(.update, id: "new"),
            .datedPage([Self.old], nextPageToken: "page-2"),
            .datedFiles([Self.new]),
            .file(.update, id: "old"),
        ])
        let result = upload(using: service, existingFileId: nil)

        XCTAssertEqual(result.fileId, "new")
        XCTAssertNil(result.error)
        XCTAssertEqual(service.executedOperations, [.search, .search, .update, .search, .search, .update])
        XCTAssertEqual(service.searchPageTokens, [nil, "page-2", nil, "page-2"])
        assertTransferredNewestAndRenamedOld(service)
    }

    func testSuccessfulEmptySearchReturnsTypedNotFoundError() {
        let service = ScriptedGoogleDriveServiceClient(steps: [.files([])])
        let client = GoogleDrivePlatformClient(service: service)
        let completion = expectation(description: "Drive download reports missing backup")
        var actualError: KotlinThrowable?

        client.downloadFile(fileName: "FeedFlow.sqlite", existingFileId: nil) { data, fileId, error in
            XCTAssertNil(data)
            actualError = error
            completion.fulfill()
        }
        wait(for: [completion], timeout: 1)

        XCTAssertTrue(actualError is CloudBackupNotFoundException)
        XCTAssertEqual(service.executedOperations, [.search])
    }

    func testCachedDownloadNotFoundAndEmptyRediscoveryReturnsTypedNotFoundError() {
        let service = ScriptedGoogleDriveServiceClient(steps: [
            .files(["stale-id"]),
            .failure(.download, code: 404),
            .files([]),
        ])
        let client = GoogleDrivePlatformClient(service: service)
        let completion = expectation(description: "Drive download reports missing backup")
        var actualError: KotlinThrowable?

        client.downloadFile(fileName: "FeedFlow.sqlite", existingFileId: "stale-id") { data, fileId, error in
            XCTAssertNil(data)
            actualError = error
            completion.fulfill()
        }
        wait(for: [completion], timeout: 1)

        XCTAssertTrue(actualError is CloudBackupNotFoundException)
        XCTAssertEqual(service.executedOperations, [.search, .download, .search])
    }

    private static let old = (id: "old", modified: Date(timeIntervalSince1970: 1_767_225_600))
    private static let new = (id: "new", modified: Date(timeIntervalSince1970: 1_769_904_000))
    private static let newerOld = (id: "old", modified: Date(timeIntervalSince1970: 1_772_323_200))

    private func duplicateUploadSteps() -> [ScriptedGoogleDriveServiceClient.Step] {
        [
            .datedFiles([Self.old, Self.new]),
            .file(.update, id: "new"),
            .datedFiles([Self.old, Self.new]),
            .file(.update, id: "old"),
        ]
    }

    private func assertTransferredNewestAndRenamedOld(
        _ service: ScriptedGoogleDriveServiceClient,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        XCTAssertEqual(service.transfers, ["new"], file: file, line: line)
        XCTAssertEqual(service.renames.map(\.fileId), ["old"], file: file, line: line)
        XCTAssertEqual(service.renames.first?.name, "FeedFlow.sqlite.orphan-old", file: file, line: line)
    }

    private func upload(
        using service: GoogleDriveServiceClient,
        existingFileId: String?
    ) -> (fileId: String?, error: KotlinThrowable?) {
        let client = GoogleDrivePlatformClient(service: service)
        let completion = expectation(description: "Drive upload completes")
        var actualFileId: String?
        var actualError: KotlinThrowable?

        client.uploadFile(
            data: Data("updated-drive-data".utf8),
            fileName: "FeedFlow.sqlite",
            existingFileId: existingFileId
        ) { fileId, error in
            actualFileId = fileId
            actualError = error
            completion.fulfill()
        }
        wait(for: [completion], timeout: 1)

        return (actualFileId, actualError)
    }
}

private final class ScriptedGoogleDriveServiceClient: GoogleDriveServiceClient {
    enum Operation: Equatable {
        case search
        case create
        case update
        case download
    }

    enum Step {
        case failure(Operation, code: Int)
        case files([String])
        case page([String], nextPageToken: String)
        case datedFiles([(id: String, modified: Date)])
        case datedPage([(id: String, modified: Date)], nextPageToken: String)
        case file(Operation, id: String)
        case data(Data)
    }

    let isAuthorized = true
    private var steps: [Step]
    private(set) var executedOperations: [Operation] = []
    private(set) var searchPageTokens: [String?] = []
    private(set) var transfers: [String] = []
    private(set) var renames: [(fileId: String, name: String?)] = []

    init(steps: [Step]) {
        self.steps = steps
    }

    func executeQuery(
        _ query: GTLRQueryProtocol,
        completionHandler: @escaping (Any?, Any?, Error?) -> Void
    ) {
        let operation = operation(for: query)
        executedOperations.append(operation)
        if let searchQuery = query as? GTLRDriveQuery_FilesList {
            searchPageTokens.append(searchQuery.pageToken)
        }
        if let getQuery = query as? GTLRDriveQuery_FilesGet {
            transfers.append(getQuery.fileId ?? "")
        }
        if let updateQuery = query as? GTLRDriveQuery_FilesUpdate {
            if updateQuery.uploadParameters != nil {
                transfers.append(updateQuery.fileId ?? "")
            } else {
                renames.append((updateQuery.fileId ?? "", (updateQuery.bodyObject as? GTLRDrive_File)?.name))
            }
        }

        guard !steps.isEmpty else {
            completionHandler(nil, nil, NSError(domain: "ScriptedDriveService", code: -1))
            return
        }

        let step = steps.removeFirst()
        switch step {
        case let .failure(expectedOperation, code):
            XCTAssertEqual(operation, expectedOperation)
            completionHandler(nil, nil, NSError(domain: "ScriptedDriveService", code: code))
        case let .files(ids):
            XCTAssertEqual(operation, .search)
            let fileList = GTLRDrive_FileList()
            fileList.files = ids.map { id in
                let file = GTLRDrive_File()
                file.identifier = id
                return file
            }
            completionHandler(nil, fileList, nil)
        case let .page(ids, nextPageToken):
            XCTAssertEqual(operation, .search)
            let fileList = GTLRDrive_FileList()
            fileList.files = ids.map { id in
                let file = GTLRDrive_File()
                file.identifier = id
                return file
            }
            fileList.nextPageToken = nextPageToken
            completionHandler(nil, fileList, nil)
        case let .datedFiles(files):
            XCTAssertEqual(operation, .search)
            completionHandler(nil, Self.fileList(files), nil)
        case let .datedPage(files, nextPageToken):
            XCTAssertEqual(operation, .search)
            let fileList = Self.fileList(files)
            fileList.nextPageToken = nextPageToken
            completionHandler(nil, fileList, nil)
        case let .file(expectedOperation, id):
            XCTAssertEqual(operation, expectedOperation)
            let file = GTLRDrive_File()
            file.identifier = id
            completionHandler(nil, file, nil)
        case let .data(data):
            XCTAssertEqual(operation, .download)
            let dataObject = GTLRDataObject()
            dataObject.data = data
            completionHandler(nil, dataObject, nil)
        }
    }

    private static func fileList(_ files: [(id: String, modified: Date)]) -> GTLRDrive_FileList {
        let fileList = GTLRDrive_FileList()
        fileList.files = files.map { entry in
            let file = GTLRDrive_File()
            file.identifier = entry.id
            file.modifiedTime = GTLRDateTime(date: entry.modified)
            return file
        }
        return fileList
    }

    private func operation(for query: GTLRQueryProtocol) -> Operation {
        switch query {
        case is GTLRDriveQuery_FilesList:
            return .search
        case is GTLRDriveQuery_FilesCreate:
            return .create
        case is GTLRDriveQuery_FilesUpdate:
            return .update
        case is GTLRDriveQuery_FilesGet:
            return .download
        default:
            XCTFail("Unexpected Drive query: \(query)")
            return .search
        }
    }
}

private final class FakeGoogleDriveServiceClient: GoogleDriveServiceClient {
    struct RemoteFile {
        let id: String
        let data: Data
    }

    let isAuthorized = true
    var remoteFiles: [String: RemoteFile] = [:]

    func executeQuery(
        _ query: GTLRQueryProtocol,
        completionHandler: @escaping (Any?, Any?, Error?) -> Void
    ) {
        if query is GTLRDriveQuery_FilesList {
            let fileList = GTLRDrive_FileList()
            if let file = remoteFiles.values.first {
                let driveFile = GTLRDrive_File()
                driveFile.identifier = file.id
                fileList.files = [driveFile]
            } else {
                fileList.files = []
            }
            completionHandler(nil, fileList, nil)
            return
        }

        if let createQuery = query as? GTLRDriveQuery_FilesCreate {
            let fileName = (createQuery.bodyObject as? GTLRDrive_File)?.name ?? "FeedFlow.sqlite"
            let data = createQuery.uploadParameters?.data ?? Data()
            remoteFiles[fileName] = RemoteFile(id: "drive-file-id", data: data)
            let driveFile = GTLRDrive_File()
            driveFile.identifier = "drive-file-id"
            completionHandler(nil, driveFile, nil)
            return
        }

        if let updateQuery = query as? GTLRDriveQuery_FilesUpdate {
            let fileName = (updateQuery.bodyObject as? GTLRDrive_File)?.name ?? "FeedFlow.sqlite"
            let data = updateQuery.uploadParameters?.data ?? Data()
            remoteFiles[fileName] = RemoteFile(id: updateQuery.fileId ?? "drive-file-id", data: data)
            let driveFile = GTLRDrive_File()
            driveFile.identifier = updateQuery.fileId
            completionHandler(nil, driveFile, nil)
            return
        }

        if query is GTLRDriveQuery_FilesGet, let file = remoteFiles.values.first {
            let dataObject = GTLRDataObject()
            dataObject.data = file.data
            completionHandler(nil, dataObject, nil)
            return
        }

        completionHandler(nil, nil, NSError(domain: "FakeGoogleDriveServiceClient", code: 1))
    }
}
