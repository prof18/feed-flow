import Foundation

struct LiveCloudSyncCommand: Decodable {
    let id: String
    let runID: String
    let action: String
    let target: String?
    let text: String?
}
