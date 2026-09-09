//
//  DropboxDataSourceIos.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 24/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import FeedFlowKit
import Foundation
import SwiftyDropbox
import UIKit

class DropboxDataSourceIos: DropboxDataSource {
    private var client: DropboxClient?
    private let injectedClient: DropboxClientBridge?
    private let documentsDirectoryURL: () -> URL
    private let logError: (String) -> Void

    init(
        client: DropboxClientBridge? = nil,
        documentsDirectoryURL: @escaping () -> URL = {
            FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        },
        logError: @escaping (String) -> Void = {
            Deps.shared.getLogger(tag: "DropboxDataSourceIos").e(messageString: $0)
        }
    ) {
        self.client = nil
        injectedClient = client
        self.documentsDirectoryURL = documentsDirectoryURL
        self.logError = logError
    }

    func setup(apiKey: String) {
        DropboxClientsManager.setupWithAppKey(
            apiKey,
            backgroundSessionIdentifier: "feed-flow-drobox-sync-background-identifier"
        ) { requestResults in
            DropboxDataSourceIos.processReconnect(requestResults: requestResults)
        }
    }

    static func processReconnect(requestResults: [Result<DropboxBaseRequestBox, ReconnectionError>]) {
        let successfulReturnedRequests = requestResults.compactMap { result -> DropboxBaseRequestBox? in
            switch result {
            case let .success(requestBox):
                return requestBox
            case .failure:
                return nil
            }
        }

        for request in successfulReturnedRequests {
            switch request {
            case let .files_upload(uploadResponse):
                uploadResponse.response { response, error in
                    if shouldAcknowledgeResumedUpload(response: response, error: error) {
                        Deps.shared.getFeedSyncRepository().onDropboxUploadSuccessAfterResume()
                    } else {
                        print("ERROR: Dropbox upload failed after resume: \(String(describing: error))")
                    }
                }

            default:
                break
            }
        }
    }

    static func shouldAcknowledgeResumedUpload<Response>(response: Response?, error: Error?) -> Bool {
        response != nil && error == nil
    }

    func startAuthorization(platformAuthHandler: @escaping () -> Void) {
        platformAuthHandler()
    }

    func handleOAuthResponse(platformOAuthResponseHandler: @escaping () -> Void) {
        platformOAuthResponseHandler()
    }

    func saveAuth(stringCredentials _: DropboxStringCredentials) {}

    func restoreAuth(stringCredentials _: DropboxStringCredentials) -> DropboxClientStatus {
        if client != nil {
            return DropboxClientStatus.notLinked
        }
        client = getClient()
        return DropboxClientStatus.linked
    }

    func isClientSet() -> Bool {
        client != nil || injectedClient != nil
    }

    func revokeAccess() async throws {
        DropboxClientsManager.unlinkClients()
        client = nil
    }

    func performDownload(
        downloadParam: DropboxDownloadParam,
        completionHandler: @escaping (DropboxDownloadResult?, Error?) -> Void
    ) {
        let destURL = documentsDirectoryURL().appendingPathComponent(downloadParam.outputName)

        let transport = injectedClient ?? getBackgroundClient().map { DropboxSDKClientBridge(client: $0) }
        if let transport {
            transport.download(path: downloadParam.path, overwrite: true, destination: destURL) { response, error in
                if let response = response {
                    print("Data successfully downloaded from Dropbox")
                    let downloadResult = DropboxDownloadResult(
                        id: response.metadata.id,
                        sizeInByte: Int64(response.metadata.size),
                        contentHash: response.metadata.contentHash,
                        destinationUrl: DatabaseDestinationUrl(url: response.destination),
                        isBackupNotFound: false,
                        revision: response.metadata.revision
                    )
                    completionHandler(downloadResult, nil)
                } else if let error = error {
                    self.logError(String(describing: error))
                    if case DropboxErrors.downloadNotFound = error {
                        completionHandler(
                            DropboxDownloadResult(
                                id: "",
                                sizeInByte: 0,
                                contentHash: nil,
                                destinationUrl: nil,
                                isBackupNotFound: true,
                                revision: nil
                            ),
                            nil
                        )
                    } else {
                        completionHandler(nil, DropboxErrors.downloadError(reason: String(describing: error)))
                    }
                }
            }
        } else {
            completionHandler(nil, DropboxErrors.downloadError(reason: "The client is nil"))
        }
    }

    func performUpload(
        uploadParam: DropboxUploadParam,
        completionHandler: @escaping (DropboxUploadResult?, Error?) -> Void
    ) {
        let transport = injectedClient ?? getClient().map { DropboxSDKClientBridge(client: $0) }
        if let transport {
            transport.upload(
                path: uploadParam.path,
                expectedRevision: uploadParam.expectedRevision,
                input: uploadParam.url
            ) { response, error in
                if let response = response {
                    print("Data successfully uploaded to Dropbox")

                    let uploadResult = DropboxUploadResult(
                        id: response.id,
                        editDateMillis: Int64(response.serverModified.timeIntervalSince1970 * 1_000),
                        sizeInByte: Int64(response.size),
                        contentHash: response.contentHash,
                        revision: response.revision,
                        isConflict: false
                    )
                    completionHandler(uploadResult, nil)
                } else if let error = error {
                    self.logError(String(describing: error))
                    if case DropboxErrors.uploadConflict = error {
                        completionHandler(
                            DropboxUploadResult(
                                id: "",
                                editDateMillis: 0,
                                sizeInByte: 0,
                                contentHash: nil,
                                revision: nil,
                                isConflict: true
                            ),
                            nil
                        )
                    } else {
                        completionHandler(nil, DropboxErrors.uploadError(reason: String(describing: error)))
                    }
                }
            }
        } else {
            completionHandler(nil, DropboxErrors.uploadError(reason: "The client is nil"))
        }
    }

    static func startAuth() {
        let scopeRequest = ScopeRequest(
            scopeType: .user,
            scopes: DropboxConstants.shared.DROPBOX_SCOPES,
            includeGrantedScopes: false
        )

        #if APP_EXTENSION
            // no-op
            print("Dropbox authorization is not supported in app extensions")
        #else
            let rootVC = UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
                .first
            let topVC = rootVC?.topmostPresented

            DropboxClientsManager.authorizeFromControllerV2(
                UIApplication.shared,
                controller: topVC,
                loadingStatusDelegate: nil,
                openURL: { url in
                    UIApplication.shared.open(url)
                },
                scopeRequest: scopeRequest
            )
        #endif
    }

    static func handleOAuthResponse(
        url: URL,
        onSuccess: @escaping () -> Void,
        onCancel: @escaping () -> Void,
        onError: @escaping () -> Void
    ) {
        let oauthCompletion: DropboxOAuthCompletion = {
            if let authResult = $0 {
                switch authResult {
                case .success:
                    print("Success! User is logged into DropboxClientsManager.")
                    onSuccess()
                case .cancel:
                    print("Authorization flow was manually canceled by user!")
                    onCancel()
                case let .error(_, description):
                    print("Error during dropbox auth:: \(String(describing: description))")
                    onError()
                }
            }
        }
        DropboxClientsManager.handleRedirectURL(url, includeBackgroundClient: false, completion: oauthCompletion)
    }

    private func getClient() -> DropboxClient? {
        DropboxClientsManager.authorizedClient ?? DropboxClientsManager.authorizedBackgroundClient
    }

    private func getBackgroundClient() -> DropboxClient? {
        DropboxClientsManager.authorizedBackgroundClient ?? DropboxClientsManager.authorizedClient
    }
}
