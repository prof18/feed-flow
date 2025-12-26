//
//  GoogleDrivePlatformClient.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 26/12/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import GoogleSignIn
import GoogleAPIClientForREST_Drive

class GoogleDrivePlatformClient: GoogleDrivePlatformClientIos {

    private var service: GTLRDriveService?

    func authenticate(onResult: @escaping (KotlinBoolean) -> Void) {
        #if APP_EXTENSION
            print("Google Drive authorization is not supported in app extensions")
            onResult(KotlinBoolean(value: false))
        #else
            guard let rootVC = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController })
                .first else {
                    onResult(KotlinBoolean(value: false))
                    return
            }

            let scopes = [kGTLRAuthScopeDriveAppdata]

            GIDSignIn.sharedInstance.signIn(
                withPresenting: rootVC,
                hint: nil,
                additionalScopes: scopes,
                nonce: nil
            ) { [weak self] result, error in
                guard let self = self, let user = result?.user, error == nil else {
                    if let error = error {
                        print("GoogleDrive: Error during sign-in: \(error.localizedDescription)")
                    }
                    onResult(KotlinBoolean(value: false))
                    return
                }

                let newService = GTLRDriveService()
                newService.authorizer = user.fetcherAuthorizer
                self.service = newService
                onResult(KotlinBoolean(value: true))
            }
        #endif
    }

    func restorePreviousSignIn(onResult: @escaping (KotlinBoolean) -> Void) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { [weak self] user, error in
            if error != nil {
                onResult(KotlinBoolean(value: false))
                return
            }

            if let user = user {
                let scopes = user.grantedScopes ?? []
                if scopes.contains(kGTLRAuthScopeDriveAppdata) {
                    let newService = GTLRDriveService()
                    newService.authorizer = user.fetcherAuthorizer
                    self?.service = newService
                    onResult(KotlinBoolean(value: true))
                } else {
                    onResult(KotlinBoolean(value: false))
                }
            } else {
                onResult(KotlinBoolean(value: false))
            }
        }
    }

    func isAuthorized() -> Bool {
        return service?.authorizer != nil && GIDSignIn.sharedInstance.currentUser != nil
    }

    func isServiceSet() -> Bool {
        return service != nil
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
        service?.authorizer = nil
        service = nil
    }

    func uploadFile(
        data: Data,
        fileName: String,
        existingFileId: String?,
        completionHandler: @escaping @Sendable (String?, KotlinThrowable?) -> Void
    ) {
        guard let service = service else {
            completionHandler(nil, GoogleDriveUploadException(errorMessage: "Drive service not initialized", exceptionCause: nil))
            return
        }

        if let fileId = existingFileId {
            updateFile(service: service, fileId: fileId, fileName: fileName, data: data, completionHandler: completionHandler)
        } else {
            searchAndUpload(service: service, fileName: fileName, data: data, completionHandler: completionHandler)
        }
    }

    private func searchAndUpload(
        service: GTLRDriveService,
        fileName: String,
        data: Data,
        completionHandler: @escaping (String?, KotlinThrowable?) -> Void
    ) {
        let searchQuery = GTLRDriveQuery_FilesList.query()
        searchQuery.q = "name='\(fileName)' and trashed=false"
        searchQuery.spaces = "appDataFolder"
        searchQuery.fields = "files(id)"

        service.executeQuery(searchQuery) { [weak self] _, result, error in
            if error != nil {
                self?.createNewFile(service: service, fileName: fileName, data: data, completionHandler: completionHandler)
                return
            }

            let fileList = result as? GTLRDrive_FileList
            if let existingFile = fileList?.files?.first, let fileId = existingFile.identifier {
                self?.updateFile(service: service, fileId: fileId, fileName: fileName, data: data, completionHandler: completionHandler)
            } else {
                self?.createNewFile(service: service, fileName: fileName, data: data, completionHandler: completionHandler)
            }
        }
    }

    private func updateFile(
        service: GTLRDriveService,
        fileId: String,
        fileName: String,
        data: Data,
        completionHandler: @escaping (String?, KotlinThrowable?) -> Void
    ) {
        let file = GTLRDrive_File()
        file.name = fileName

        let uploadParameters = GTLRUploadParameters(data: data, mimeType: "application/x-sqlite3")
        let updateQuery = GTLRDriveQuery_FilesUpdate.query(withObject: file, fileId: fileId, uploadParameters: uploadParameters)
        updateQuery.fields = "id"

        service.executeQuery(updateQuery) { [weak self] _, result, error in
            if error != nil {
                self?.createNewFile(service: service, fileName: fileName, data: data, completionHandler: completionHandler)
                return
            }

            if let uploadedFile = result as? GTLRDrive_File {
                completionHandler(uploadedFile.identifier, nil)
            } else {
                completionHandler(fileId, nil)
            }
        }
    }

    private func createNewFile(
        service: GTLRDriveService,
        fileName: String,
        data: Data,
        completionHandler: @escaping (String?, KotlinThrowable?) -> Void
    ) {
        let file = GTLRDrive_File()
        file.name = fileName
        file.parents = ["appDataFolder"]

        let uploadParameters = GTLRUploadParameters(data: data, mimeType: "application/x-sqlite3")
        let createQuery = GTLRDriveQuery_FilesCreate.query(withObject: file, uploadParameters: uploadParameters)
        createQuery.fields = "id"

        service.executeQuery(createQuery) { _, result, error in
            if let error = error {
                completionHandler(nil, GoogleDriveUploadException(errorMessage: error.localizedDescription, exceptionCause: nil))
                return
            }

            if let uploadedFile = result as? GTLRDrive_File {
                completionHandler(uploadedFile.identifier, nil)
            } else {
                completionHandler(nil, nil)
            }
        }
    }

    func downloadFile(
        fileName: String,
        existingFileId: String?,
        completionHandler: @escaping @Sendable (Data?, KotlinThrowable?) -> Void
    ) {
        guard let service = service else {
            completionHandler(nil, GoogleDriveDownloadException(errorMessage: "Drive service not initialized", exceptionCause: nil))
            return
        }

        if let fileId = existingFileId {
            downloadFileById(service: service, fileId: fileId, completionHandler: completionHandler)
        } else {
            searchAndDownload(service: service, fileName: fileName, completionHandler: completionHandler)
        }
    }

    private func searchAndDownload(
        service: GTLRDriveService,
        fileName: String,
        completionHandler: @escaping @Sendable (Data?, KotlinThrowable?) -> Void
    ) {
        let query = GTLRDriveQuery_FilesList.query()
        query.q = "name='\(fileName)' and trashed=false"
        query.spaces = "appDataFolder"
        query.fields = "files(id)"

        service.executeQuery(query) { [weak self] _, result, error in
            if let error = error {
                completionHandler(nil, GoogleDriveDownloadException(errorMessage: error.localizedDescription, exceptionCause: nil))
                return
            }

            guard let fileList = result as? GTLRDrive_FileList,
                  let file = fileList.files?.first,
                  let fileId = file.identifier else {
                completionHandler(nil, GoogleDriveDownloadException(errorMessage: "File not found", exceptionCause: nil))
                return
            }

            self?.downloadFileById(service: service, fileId: fileId, completionHandler: completionHandler)
        }
    }

    private func downloadFileById(
        service: GTLRDriveService,
        fileId: String,
        completionHandler: @escaping @Sendable (Data?, KotlinThrowable?) -> Void
    ) {
        let downloadQuery = GTLRDriveQuery_FilesGet.queryForMedia(withFileId: fileId)

        service.executeQuery(downloadQuery) { _, fileData, error in
            if let error = error {
                completionHandler(nil, GoogleDriveDownloadException(errorMessage: error.localizedDescription, exceptionCause: nil))
                return
            }

            guard let data = (fileData as? GTLRDataObject)?.data else {
                completionHandler(nil, GoogleDriveDownloadException(errorMessage: "No data received", exceptionCause: nil))
                return
            }

            completionHandler(data, nil)
        }
    }

    static func handleOAuthResponse(url: URL) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
