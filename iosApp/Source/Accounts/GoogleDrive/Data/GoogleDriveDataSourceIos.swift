//
//  GoogleDriveDataSourceIos.swift
//  FeedFlow
//
//  Created by Claude on 05/11/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import GoogleSignIn
import GoogleAPIClientForREST_Drive

class GoogleDriveDataSourceIos: GoogleDriveDataSource {
    private var driveService: GTLRDriveService?
    private var clientId: String?

    func setup(clientId: String) {
        self.clientId = clientId
        driveService = GTLRDriveService()
    }

    func startAuthorization(platformAuthHandler: @escaping () -> Void) {
        platformAuthHandler()
    }

    func handleOAuthResponse(platformOAuthResponseHandler: @escaping () -> Void) {
        platformOAuthResponseHandler()
    }

    func saveAuth(stringCredentials: GoogleDriveStringCredentials) {
        // OAuth is handled by Google Sign-In SDK, no need to manually save
    }

    func restoreAuth(stringCredentials: GoogleDriveStringCredentials) -> GoogleDriveClientStatus {
        guard let user = GIDSignIn.sharedInstance.currentUser else {
            return GoogleDriveClientStatus.notLinked
        }

        driveService?.authorizer = user.fetcherAuthorizer
        return GoogleDriveClientStatus.linked
    }

    func isClientSet() -> Bool {
        driveService?.authorizer != nil
    }

    func revokeAccess() async throws {
        GIDSignIn.sharedInstance.signOut()
        driveService?.authorizer = nil
    }

    func performDownload(
        downloadParam: GoogleDriveDownloadParam,
        completionHandler: @escaping (GoogleDriveDownloadResult?, Error?) -> Void
    ) {
        guard let service = driveService else {
            completionHandler(nil, GoogleDriveErrors.downloadError(reason: "Drive service not initialized"))
            return
        }

        // Search for the file in appDataFolder
        let query = GTLRDriveQuery_FilesList.query()
        query.q = "name='\(downloadParam.path.components(separatedBy: "/").last ?? "")'"
        query.spaces = "appDataFolder"
        query.fields = "files(id, size)"

        service.executeQuery(query) { _, result, error in
            if let error = error {
                Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                    messageString: "Error searching for file: \(error.localizedDescription)"
                )
                completionHandler(nil, GoogleDriveErrors.downloadError(reason: error.localizedDescription))
                return
            }

            guard let fileList = result as? GTLRDrive_FileList,
                  let files = fileList.files,
                  let file = files.first else {
                completionHandler(nil, GoogleDriveErrors.downloadError(reason: "File not found"))
                return
            }

            // Download the file
            let fileManager = FileManager.default
            let directoryURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
            let destURL = directoryURL.appendingPathComponent(downloadParam.outputName)

            let downloadQuery = GTLRDriveQuery_FilesGet.queryForMedia(withFileId: file.identifier ?? "")
            service.executeQuery(downloadQuery) { _, fileData, error in
                if let error = error {
                    Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                        messageString: "Error downloading file: \(error.localizedDescription)"
                    )
                    completionHandler(nil, GoogleDriveErrors.downloadError(reason: error.localizedDescription))
                    return
                }

                guard let data = (fileData as? GTLRDataObject)?.data else {
                    completionHandler(nil, GoogleDriveErrors.downloadError(reason: "No data received"))
                    return
                }

                do {
                    try data.write(to: destURL)
                    print("Data successfully downloaded from Google Drive")

                    let downloadResult = GoogleDriveDownloadResult(
                        id: file.identifier ?? "",
                        sizeInByte: file.size?.int64Value ?? 0,
                        destinationUrl: DatabaseDestinationUrl(url: destURL as NSURL)
                    )
                    completionHandler(downloadResult, nil)
                } catch {
                    completionHandler(nil, GoogleDriveErrors.downloadError(reason: error.localizedDescription))
                }
            }
        }
    }

    func performUpload(
        uploadParam: GoogleDriveUploadParam,
        completionHandler: @escaping (GoogleDriveUploadResult?, Error?) -> Void
    ) {
        guard let service = driveService else {
            completionHandler(nil, GoogleDriveErrors.uploadError(reason: "Drive service not initialized"))
            return
        }

        let fileName = uploadParam.path.components(separatedBy: "/").last ?? "feedflow.db"

        // First, check if file exists
        let searchQuery = GTLRDriveQuery_FilesList.query()
        searchQuery.q = "name='\(fileName)'"
        searchQuery.spaces = "appDataFolder"
        searchQuery.fields = "files(id)"

        service.executeQuery(searchQuery) { _, searchResult, searchError in
            if let searchError = searchError {
                Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                    messageString: "Error searching for existing file: \(searchError.localizedDescription)"
                )
                completionHandler(nil, GoogleDriveErrors.uploadError(reason: searchError.localizedDescription))
                return
            }

            guard let url = uploadParam.url as URL? else {
                completionHandler(nil, GoogleDriveErrors.uploadError(reason: "Invalid file URL"))
                return
            }

            guard let data = try? Data(contentsOf: url) else {
                completionHandler(nil, GoogleDriveErrors.uploadError(reason: "Could not read file data"))
                return
            }

            let file = GTLRDrive_File()
            file.name = fileName

            let uploadParameters = GTLRUploadParameters(data: data, mimeType: "application/octet-stream")

            let fileList = searchResult as? GTLRDrive_FileList
            let existingFiles = fileList?.files

            let uploadQuery: GTLRDriveQuery
            if let existingFile = existingFiles?.first, let fileId = existingFile.identifier {
                // Update existing file
                uploadQuery = GTLRDriveQuery_FilesUpdate.query(
                    withObject: file,
                    fileId: fileId,
                    uploadParameters: uploadParameters
                )
            } else {
                // Create new file in appDataFolder
                file.parents = ["appDataFolder"]
                uploadQuery = GTLRDriveQuery_FilesCreate.query(
                    withObject: file,
                    uploadParameters: uploadParameters
                )
            }

            (uploadQuery as? GTLRDriveQuery_FilesUpdate)?.fields = "id, modifiedTime, size"
            (uploadQuery as? GTLRDriveQuery_FilesCreate)?.fields = "id, modifiedTime, size"

            service.executeQuery(uploadQuery) { _, result, error in
                if let error = error {
                    Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                        messageString: "Error uploading file: \(error.localizedDescription)"
                    )
                    completionHandler(nil, GoogleDriveErrors.uploadError(reason: error.localizedDescription))
                    return
                }

                guard let uploadedFile = result as? GTLRDrive_File else {
                    completionHandler(nil, GoogleDriveErrors.uploadError(reason: "Invalid upload response"))
                    return
                }

                print("Data successfully uploaded to Google Drive")

                let uploadResult = GoogleDriveUploadResult(
                    id: uploadedFile.identifier ?? "",
                    editDateMillis: Int64((uploadedFile.modifiedTime?.date.timeIntervalSince1970 ?? 0) * 1_000),
                    sizeInByte: uploadedFile.size?.int64Value ?? 0
                )
                completionHandler(uploadResult, nil)
            }
        }
    }

    static func startAuth(clientId: String, presentingViewController: UIViewController) {
        #if APP_EXTENSION
            print("Google Drive authorization is not supported in app extensions")
        #else
            let signInConfig = GIDConfiguration(clientID: clientId)
            GIDSignIn.sharedInstance.configuration = signInConfig

            GIDSignIn.sharedInstance.signIn(
                withPresenting: presentingViewController,
                hint: nil,
                additionalScopes: GoogleDriveConstants.shared.GOOGLE_DRIVE_SCOPES
            ) { signInResult, error in
                if let error = error {
                    Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                        messageString: "Error during sign-in: \(error.localizedDescription)"
                    )
                    return
                }

                guard let user = signInResult?.user else {
                    Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                        messageString: "User is nil after sign-in"
                    )
                    return
                }

                print("Successfully signed in to Google Drive")
            }
        #endif
    }

    static func handleOAuthResponse(
        url: URL,
        onSuccess: @escaping () -> Void,
        onCancel: @escaping () -> Void,
        onError: @escaping () -> Void
    ) {
        GIDSignIn.sharedInstance.handle(url)
    }

    static func restorePreviousSignIn(
        onSuccess: @escaping () -> Void,
        onError: @escaping () -> Void
    ) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, error in
            if let error = error {
                Deps.shared.getLogger(tag: "GoogleDriveDataSourceIos").e(
                    messageString: "Error restoring sign-in: \(error.localizedDescription)"
                )
                onError()
                return
            }

            if user != nil {
                onSuccess()
            } else {
                onError()
            }
        }
    }
}
