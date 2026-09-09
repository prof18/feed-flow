import Foundation

struct DropboxFileMetadata {
    let id: String
    let size: UInt64
    let serverModified: Date
    let contentHash: String?
    let revision: String
}
