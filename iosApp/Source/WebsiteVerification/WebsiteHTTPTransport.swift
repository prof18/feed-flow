import Foundation

final class WebsiteHTTPTransport {
    private let protocolClasses: [AnyClass]?

    init(protocolClasses: [AnyClass]? = nil) {
        self.protocolClasses = protocolClasses
    }

    /// Sends exactly one request: a redirect is returned as a response and never followed.
    func fetch(request: URLRequest, maxBytes: Int) async throws -> WebsiteHTTPResponse {
        guard maxBytes > 0, let url = request.url, Self.isSafeWebURL(url) else {
            throw WebsiteHTTPTransportError.invalidURL
        }
        let operation = WebsiteHTTPTransportOperation(maxBytes: maxBytes, protocolClasses: protocolClasses)
        return try await withTaskCancellationHandler {
            try await operation.fetch(request: request)
        } onCancel: {
            operation.cancel()
        }
    }

    static func isSafeWebURL(_ url: URL) -> Bool {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let scheme = components.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              let host = components.host, !host.isEmpty,
              components.user == nil, components.password == nil,
              !host.contains("%"), host.allSatisfy(\.isASCII),
              validHostCharacters(host)
        else { return false }
        return true
    }

    private static func validHostCharacters(_ host: String) -> Bool {
        if host.contains(":") {
            return host.allSatisfy { $0.isHexDigit || $0 == ":" || $0 == "." }
        }
        return host.allSatisfy { $0.isLetter || $0.isNumber || $0 == "." || $0 == "-" }
    }
}
