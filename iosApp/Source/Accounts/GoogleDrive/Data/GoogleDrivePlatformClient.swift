//
//  GoogleDrivePlatformClient.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 26/12/25.
//  Copyright © 2025 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import GoogleAPIClientForREST_Drive
import GoogleSignIn

class GoogleDrivePlatformClient: GoogleDrivePlatformClientIos {
    private var service: GoogleDriveServiceClient?

    init(service: GoogleDriveServiceClient? = nil) {
        self.service = service
    }

    func authenticate(onResult: @escaping (KotlinBoolean) -> Void) {
        #if APP_EXTENSION
            print("Google Drive authorization is not supported in app extensions")
            onResult(KotlinBoolean(value: false))
        #else
            guard let rootVC = UIApplication.shared.connectedScenes
                .compactMap({ ($0 as? UIWindowScene)?.keyWindow?.rootViewController })
                .first
            else {
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
                self.service = GoogleDriveServiceClientImpl(service: newService)
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
                let newService = GTLRDriveService()
                newService.authorizer = user.fetcherAuthorizer
                self?.service = GoogleDriveServiceClientImpl(service: newService)
                onResult(KotlinBoolean(value: true))
            } else {
                onResult(KotlinBoolean(value: false))
            }
        }
    }

    func isAuthorized() -> Bool {
        return service?.isAuthorized == true && GIDSignIn.sharedInstance.currentUser != nil
    }

    func isServiceSet() -> Bool {
        return service != nil
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
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

        searchAndUpload(
            service: service,
            fileName: fileName,
            data: data,
            rediscoverOnNotFound: true,
            completionHandler: completionHandler
        )
    }

    func downloadFile(
        fileName: String,
        existingFileId: String?,
        completionHandler: @escaping @Sendable (Data?, String?, KotlinThrowable?) -> Void
    ) {
        guard let service = service else {
            completionHandler(nil, nil, GoogleDriveDownloadException(errorMessage: "Drive service not initialized", exceptionCause: nil))
            return
        }

        searchAndDownload(
            service: service,
            fileName: fileName,
            rediscoverOnNotFound: true,
            completionHandler: completionHandler
        )
    }

    private func searchAndUpload(
        service: GoogleDriveServiceClient,
        fileName: String,
        data: Data,
        rediscoverOnNotFound: Bool,
        completionHandler: @escaping (String?, KotlinThrowable?) -> Void
    ) {
        discoverFiles(service: service, fileName: fileName) { [weak self] result in
            switch result {
            case let .failure(error):
                completionHandler(
                    nil,
                    GoogleDriveUploadException(errorMessage: error.localizedDescription, exceptionCause: nil)
                )
            case let .success(files):
                guard let resolution = Self.resolve(files) else {
                    self?.createNewFile(service: service, fileName: fileName, data: data, completionHandler: completionHandler)
                    return
                }

                self?.updateFile(
                    service: service,
                    fileId: resolution.keptId,
                    fileName: fileName,
                    data: data,
                    rediscoverOnNotFound: rediscoverOnNotFound,
                    hasOrphans: !resolution.orphanIds.isEmpty,
                    completionHandler: completionHandler
                )
            }
        }
    }

    private func updateFile(
        service: GoogleDriveServiceClient,
        fileId: String,
        fileName: String,
        data: Data,
        rediscoverOnNotFound: Bool,
        hasOrphans: Bool = false,
        completionHandler: @escaping (String?, KotlinThrowable?) -> Void
    ) {
        let file = GTLRDrive_File()
        file.name = fileName

        let uploadParameters = GTLRUploadParameters(data: data, mimeType: "application/x-sqlite3")
        let updateQuery = GTLRDriveQuery_FilesUpdate.query(withObject: file, fileId: fileId, uploadParameters: uploadParameters)
        updateQuery.fields = "id"

        service.executeQuery(updateQuery) { [weak self] _, result, error in
            if let error {
                if rediscoverOnNotFound, Self.isNotFound(error) {
                    self?.searchAndUpload(
                        service: service,
                        fileName: fileName,
                        data: data,
                        rediscoverOnNotFound: false,
                        completionHandler: completionHandler
                    )
                } else {
                    completionHandler(
                        nil,
                        GoogleDriveUploadException(errorMessage: error.localizedDescription, exceptionCause: nil)
                    )
                }
                return
            }

            guard let uploadedFile = result as? GTLRDrive_File,
                  let uploadedFileId = uploadedFile.identifier
            else {
                completionHandler(
                    nil,
                    GoogleDriveUploadException(errorMessage: "Invalid file update response", exceptionCause: nil)
                )
                return
            }

            guard hasOrphans, let self else {
                completionHandler(uploadedFileId, nil)
                return
            }
            self.renameOrphans(service: service, fileName: fileName, keptId: fileId) {
                completionHandler(uploadedFileId, nil)
            }
        }
    }

    private func createNewFile(
        service: GoogleDriveServiceClient,
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
                guard let uploadedFileId = uploadedFile.identifier else {
                    completionHandler(
                        nil,
                        GoogleDriveUploadException(errorMessage: "Created file has no identifier", exceptionCause: nil)
                    )
                    return
                }
                completionHandler(uploadedFileId, nil)
            } else {
                completionHandler(
                    nil,
                    GoogleDriveUploadException(errorMessage: "Invalid file creation response", exceptionCause: nil)
                )
            }
        }
    }

    private func searchAndDownload(
        service: GoogleDriveServiceClient,
        fileName: String,
        rediscoverOnNotFound: Bool,
        completionHandler: @escaping @Sendable (Data?, String?, KotlinThrowable?) -> Void
    ) {
        discoverFiles(service: service, fileName: fileName) { [weak self] result in
            switch result {
            case let .failure(error):
                completionHandler(nil, nil, GoogleDriveDownloadException(errorMessage: error.localizedDescription, exceptionCause: nil))
            case let .success(files):
                guard let resolution = Self.resolve(files) else {
                    completionHandler(nil, nil, CloudBackupNotFoundException())
                    return
                }

                self?.downloadFileById(
                    service: service,
                    fileId: resolution.keptId,
                    fileName: fileName,
                    rediscoverOnNotFound: rediscoverOnNotFound,
                    hasOrphans: !resolution.orphanIds.isEmpty,
                    completionHandler: completionHandler
                )
            }
        }
    }

    private func discoverFiles(
        service: GoogleDriveServiceClient,
        fileName: String,
        files: [GTLRDrive_File] = [],
        pageToken: String? = nil,
        completionHandler: @escaping (Result<[GTLRDrive_File], Error>) -> Void
    ) {
        let query = GTLRDriveQuery_FilesList.query()
        query.q = "name='\(fileName)' and trashed=false"
        query.spaces = "appDataFolder"
        query.fields = "nextPageToken,files(id,modifiedTime)"
        query.pageToken = pageToken

        service.executeQuery(query) { [weak self] _, result, error in
            if let error {
                completionHandler(.failure(error))
                return
            }

            guard let fileList = result as? GTLRDrive_FileList else {
                completionHandler(.failure(GoogleDriveDiscoveryError.invalidResponse))
                return
            }

            let discoveredFiles = files + (fileList.files ?? [])

            if let nextPageToken = fileList.nextPageToken, !nextPageToken.isEmpty {
                self?.discoverFiles(
                    service: service,
                    fileName: fileName,
                    files: discoveredFiles,
                    pageToken: nextPageToken,
                    completionHandler: completionHandler
                )
            } else {
                completionHandler(.success(discoveredFiles))
            }
        }
    }

    private func renameOrphans(
        service: GoogleDriveServiceClient,
        fileName: String,
        keptId: String,
        completion: @escaping () -> Void
    ) {
        discoverFiles(service: service, fileName: fileName) { [weak self] result in
            guard case let .success(files) = result,
                  let resolution = Self.resolve(files),
                  resolution.keptId == keptId,
                  let self
            else {
                completion()
                return
            }
            self.renameNext(service: service, fileName: fileName, orphanIds: resolution.orphanIds, completion: completion)
        }
    }

    private func renameNext(
        service: GoogleDriveServiceClient,
        fileName: String,
        orphanIds: [String],
        completion: @escaping () -> Void
    ) {
        guard let orphanId = orphanIds.first else {
            completion()
            return
        }

        let file = GTLRDrive_File()
        file.name = GoogleDriveBackupResolver.shared.orphanName(fileName: fileName, fileId: orphanId)
        let renameQuery = GTLRDriveQuery_FilesUpdate.query(withObject: file, fileId: orphanId, uploadParameters: nil)

        service.executeQuery(renameQuery) { [weak self] _, _, _ in
            guard let self else {
                completion()
                return
            }
            self.renameNext(
                service: service,
                fileName: fileName,
                orphanIds: Array(orphanIds.dropFirst()),
                completion: completion
            )
        }
    }

    private func downloadFileById(
        service: GoogleDriveServiceClient,
        fileId: String,
        fileName: String,
        rediscoverOnNotFound: Bool,
        hasOrphans: Bool = false,
        completionHandler: @escaping @Sendable (Data?, String?, KotlinThrowable?) -> Void
    ) {
        let downloadQuery = GTLRDriveQuery_FilesGet.queryForMedia(withFileId: fileId)

        service.executeQuery(downloadQuery) { [weak self] _, fileData, error in
            if let error = error {
                if rediscoverOnNotFound, Self.isNotFound(error) {
                    self?.searchAndDownload(
                        service: service,
                        fileName: fileName,
                        rediscoverOnNotFound: false,
                        completionHandler: completionHandler
                    )
                } else {
                    completionHandler(
                        nil,
                        nil,
                        GoogleDriveDownloadException(errorMessage: error.localizedDescription, exceptionCause: nil)
                    )
                }
                return
            }

            guard let data = (fileData as? GTLRDataObject)?.data else {
                completionHandler(nil, nil, GoogleDriveDownloadException(errorMessage: "No data received", exceptionCause: nil))
                return
            }

            guard hasOrphans, let self else {
                completionHandler(data, fileId, nil)
                return
            }
            self.renameOrphans(service: service, fileName: fileName, keptId: fileId) {
                completionHandler(data, fileId, nil)
            }
        }
    }

    private static func resolve(_ files: [GTLRDrive_File]) -> GoogleDriveBackupResolution? {
        let candidates = files.compactMap { file -> GoogleDriveBackupCandidate? in
            guard let id = file.identifier else { return nil }
            let millis = Int64(((file.modifiedTime?.date.timeIntervalSince1970 ?? 0) * 1_000).rounded())
            return GoogleDriveBackupCandidate(id: id, modifiedTimeMillis: millis)
        }
        return GoogleDriveBackupResolver.shared.resolve(candidates: candidates)
    }

    private static func isNotFound(_ error: Error) -> Bool {
        (error as NSError).code == 404
    }
}

private enum GoogleDriveDiscoveryError: LocalizedError {
    case invalidResponse

    var errorDescription: String? {
        switch self {
        case .invalidResponse:
            return "Invalid file search response"
        }
    }
}
