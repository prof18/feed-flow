//
//  DropboxDataSourceIos.swift
//  FeedFlow
//
//  Created by Marco Gomiero on 24/06/24.
//  Copyright © 2024 FeedFlow. All rights reserved.
//

import Foundation
import shared
import SwiftyDropbox
import UIKit

class DropboxDataSourceIos: DropboxDataSource {
    private var client: DropboxClient?

    func setup(apiKey: String) {
        DropboxClientsManager.setupWithAppKey(
            apiKey,
            backgroundSessionIdentifier: "feed-flow-drobox-sync-background-identifier",
            requestsToReconnect: { requestResults in
                DropboxDataSourceIos.processReconnect(requestResults: requestResults)
            }
        )
    }

    static func processReconnect(requestResults: ([Result<DropboxBaseRequestBox, ReconnectionError>])) {
        let successfulReturnedRequests = requestResults.compactMap { result -> DropboxBaseRequestBox? in
            switch result {
            case .success(let requestBox):
                return requestBox
            case .failure:
                return nil
            }
        }

        for request in successfulReturnedRequests {
            switch request {
            case .upload(let uploadResponse):
                uploadResponse.response { _, error in
                    // handle response
                    if error != nil {
                        KotlinDependencies.shared.getFeedSyncRepository().onDropboxUploadSuccessAfterResume()
                    } else {
                        print("ERROR: Upload error after resume")
                    }
                }

            default:
                break
            }
        }
    }

    func startAuthorization(platformAuthHandler: @escaping () -> Void) {
        platformAuthHandler()
    }

    func handleOAuthResponse(platformOAuthResponseHandler: @escaping () -> Void) {
        platformOAuthResponseHandler()
    }

    func saveAuth(stringCredentials: DropboxStringCredentials) {

    }

    func restoreAuth(stringCredentials: DropboxStringCredentials) -> DropboxClientStatus {
        if client != nil {
            return DropboxClientStatus.notLinked
        }
        client = getClient()
        return DropboxClientStatus.linked
    }

    func isClientSet() -> Bool {
        client != nil
    }

    func revokeAccess() async throws {
        DropboxClientsManager.unlinkClients()
        client = nil
    }

    func performDownload(
        downloadParam: DropboxDownloadParam,
        completionHandler: @escaping (DropboxDownloadResult?, Error?) -> Void
    ) {
        let fileManager = FileManager.default
        let directoryURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let destURL = directoryURL.appendingPathComponent(downloadParam.outputName)

        if let client = getBackgroundClient() {
            client.files.download(path: downloadParam.path, overwrite: true, destination: destURL)
                .response { response, error in
                    if let response = response {
                        print("Data successfully downloaded from Dropbox")
                        let downloadResult = DropboxDownloadResult(
                            id: response.0.id,
                            sizeInByte: Int64(response.0.size),
                            contentHash: response.0.contentHash,
                            destinationUrl: DatabaseDestinationUrl(url: response.1)
                        )
                        completionHandler(downloadResult, nil)

                    } else if let error = error {
                        KotlinDependencies.shared.getLogger(tag: "DropboxDataSourceIos").e(
                            messageString: error.description
                        )

                        switch error as CallError {
                        case .routeError(let boxed, _, _, _):
                            let err = boxed.unboxed as Files.DownloadError
                            KotlinDependencies.shared.getLogger(tag: "DropboxDataSourceIos").e(
                                messageString: "Boxed error: \(err.description)"
                            )

                        default:
                            break
                        }

                        completionHandler(nil, DropboxErrors.downloadError(reason: error.description))
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
        if let client = getClient() {
            client.files.upload(
                path: uploadParam.path,
                mode: .overwrite,
                input: uploadParam.url
            )
            .response { response, error in
                if let response = response {
                    print("Data successfully uploaded to Dropbox")

                    let uploadResult = DropboxUploadResult(
                        id: response.id,
                        editDateMillis: Int64(response.serverModified.timeIntervalSince1970 * 1000),
                        sizeInByte: Int64(response.size),
                        contentHash: response.contentHash
                    )
                    completionHandler(uploadResult, nil)
                } else if let error = error {
                    KotlinDependencies.shared.getLogger(tag: "DropboxDataSourceIos").e(
                        messageString: error.description
                    )

                    switch error as CallError {
                    case .routeError(let boxed, _, _, _):
                        let err = boxed.unboxed as Files.UploadError
                        KotlinDependencies.shared.getLogger(tag: "DropboxDataSourceIos").e(
                            messageString: "Boxed error: \(err.description)"
                        )

                    default:
                        break
                    }

                    completionHandler(nil, DropboxErrors.uploadError(reason: error.description))
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

        DropboxClientsManager.authorizeFromControllerV2(
            UIApplication.shared,
            controller: nil,
            loadingStatusDelegate: nil,
            openURL: { url in
                UIApplication.shared.open(url)
            },
            scopeRequest: scopeRequest
        )
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
                case .error(_, let description):
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
