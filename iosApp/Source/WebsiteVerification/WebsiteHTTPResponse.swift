import Foundation

struct WebsiteHTTPResponse {
    let effectiveURL: URL
    let statusCode: Int
    let headers: [String: String]
    let data: Data
    let challenge: Bool
    let tooLarge: Bool
}
