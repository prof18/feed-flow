import Foundation

enum WebsiteHTTPURLProtocolRoute {
    case response(status: Int, headers: [String: String], body: Data)
    case headersOnly(status: Int, headers: [String: String])
    case redirect(to: URL, headers: [String: String] = [:])
    case wait
}
