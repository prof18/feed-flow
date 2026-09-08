import Foundation

protocol DropboxClientBridge {
    func upload(
        path: String,
        input: URL,
        completion: @escaping (DropboxFileMetadata?, Error?) -> Void
    )

    func download(
        path: String,
        overwrite: Bool,
        destination: URL,
        completion: @escaping (DropboxDownloadResponse?, Error?) -> Void
    )
}
