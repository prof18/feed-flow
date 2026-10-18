import Foundation

struct WebsiteBrowserCapturedResponse {
    let effectiveURL: String
    let statusCode: Int
    let contentType: String?
    let data: Data
}
