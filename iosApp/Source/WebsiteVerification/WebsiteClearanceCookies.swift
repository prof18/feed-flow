import Foundation

enum WebsiteClearanceCookies {
    static func identity(cookies: [HTTPCookie], url: URL, userAgent: String) -> WebsiteReplayIdentity? {
        guard url.scheme?.lowercased() == "https", let host = url.host?.lowercased() else { return nil }
        let path = url.path.isEmpty ? "/" : url.path
        let matching = cookies.filter { cookie in
            guard cookie.name == "cf_clearance", cookie.expiresDate.map({ $0 > Date() }) ?? true else { return false }
            let domain = cookie.domain.trimmingCharacters(in: CharacterSet(charactersIn: ".")).lowercased()
            let domainMatches = host == domain || (cookie.domain.hasPrefix(".") && host.hasSuffix(".\(domain)"))
            let pathMatches = path == cookie.path ||
                (path.hasPrefix(cookie.path) &&
                    (cookie.path.hasSuffix("/") || path.dropFirst(cookie.path.count).hasPrefix("/")))
            return domainMatches && pathMatches
        }
        guard matching.count <= 1 else { return nil }
        return WebsiteReplayIdentity(userAgent: userAgent, cookie: matching.first.map { "\($0.name)=\($0.value)" })
    }
}
