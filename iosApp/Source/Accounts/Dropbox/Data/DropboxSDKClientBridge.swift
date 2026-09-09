import FeedFlowKit
import Foundation
import SwiftyDropbox

struct DropboxSDKClientBridge: DropboxClientBridge {
    let client: DropboxClient

    func upload(
        path: String,
        expectedRevision: String?,
        input: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    ) {
        let mode = expectedRevision.map(Files.WriteMode.update) ?? .add
        client.files
            .upload(
                path: path,
                mode: mode,
                autorename: false,
                strictConflict: true,
                input: input
            )
            .response { response, error in
                if let response {
                    completion(metadata(response), nil)
                } else {
                    if let error,
                       case let .routeError(boxed, _, _, _) = error,
                       case let .path(writeFailed) = boxed.unboxed,
                       case .conflict = writeFailed.reason {
                        completion(nil, DropboxErrors.uploadConflict)
                        return
                    }
                    if let error, case let .routeError(boxed, _, _, _) = error {
                        Deps.shared.getLogger(tag: "DropboxDataSourceIos").e(
                            messageString: "Boxed error: \(boxed.unboxed.description)"
                        )
                    }
                    completion(nil, error)
                }
            }
    }

    func download(
        path: String,
        overwrite: Bool,
        destination: URL,
        completion: @escaping (DropboxDownloadResponse?, Error?) -> Void
    ) {
        client.files.download(path: path, overwrite: overwrite, destination: destination).response { response, error in
            if let response {
                completion(DropboxDownloadResponse(metadata: metadata(response.0), destination: response.1), nil)
            } else {
                if let error,
                   case let .routeError(boxed, _, _, _) = error,
                   case .path(.notFound) = boxed.unboxed {
                    completion(nil, DropboxErrors.downloadNotFound)
                    return
                }
                if let error, case let .routeError(boxed, _, _, _) = error {
                    Deps.shared.getLogger(tag: "DropboxDataSourceIos").e(
                        messageString: "Boxed error: \(boxed.unboxed.description)"
                    )
                }
                completion(nil, error)
            }
        }
    }

    private func metadata(_ file: Files.FileMetadata) -> DropboxFileMetadata {
        DropboxFileMetadata(
            id: file.id,
            size: file.size,
            serverModified: file.serverModified,
            contentHash: file.contentHash,
            revision: file.rev
        )
    }
}
