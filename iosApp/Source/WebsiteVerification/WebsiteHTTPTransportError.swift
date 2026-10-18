import Foundation

enum WebsiteHTTPTransportError: Error {
    case invalidURL
    case transport(Error)
}
