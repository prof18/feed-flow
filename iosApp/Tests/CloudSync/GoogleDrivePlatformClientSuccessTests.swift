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
        ) { data, error in
            XCTAssertNil(error)
            XCTAssertEqual(data, expectedBytes)
            downloadExpectation.fulfill()
        }
        wait(for: [downloadExpectation], timeout: 1)

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
