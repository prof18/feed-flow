import FeedFlowKit
import Foundation
import WebKit

/// Reads the identity saved in a recovery profile's WebKit data store. Nothing here runs at launch.
@MainActor
enum WebsiteProfileIdentity {
    private static let prefix = "feedflow-recovery-"

    static func identifier(_ profile: FeedRecoveryProfile) -> UUID? {
        identifier(profileId: profile.id)
    }

    static func identifier(profileId: String) -> UUID? {
        guard profileId.hasPrefix(prefix) else { return nil }
        return UUID(uuidString: String(profileId.dropFirst(prefix.count)))
    }

    static func origin(_ url: URL) -> String? {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              components.scheme?.lowercased() == "https", let host = components.host,
              components.user == nil, components.password == nil,
              HttpHostValidator.shared.isUrlSafeForHttpClient(url: url.absoluteString) else { return nil }
        return "https://\(host.lowercased()):\(components.port ?? 443)"
    }

    /// The user agent the clearance was issued to and the single matching clearance cookie.
    static func identity(profile: FeedRecoveryProfile, url: URL) async -> WebsiteReplayIdentity? {
        guard profile.origin == origin(url) else { return nil }
        return await identity(profileId: profile.id, userAgent: profile.userAgent, url: url)
    }

    static func identity(profileId: String, userAgent: String, url: URL) async -> WebsiteReplayIdentity? {
        guard origin(url) != nil, let identifier = identifier(profileId: profileId),
              await WKWebsiteDataStore.allDataStoreIdentifiers.contains(identifier) else { return nil }
        let store = WKWebsiteDataStore(forIdentifier: identifier)
        let cookies = await store.httpCookieStore.allCookies()
        return WebsiteClearanceCookies.identity(cookies: cookies, url: url, userAgent: userAgent)
    }
}
