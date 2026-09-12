import Foundation

struct LiveCloudSyncResponse: Encodable {
    let id: String
    let runID: String
    let ok: Bool
    let error: String?
    let tree: String
    let screenshot: String?
}
