import Foundation
import WebKit

struct WebKitWebsiteDataStoreProvider: WebsiteDataStoreProviding {
    @MainActor private static var webKitStarted = false

    @MainActor
    func identifiers() async -> [UUID] {
        await Self.allIdentifiers()
    }

    @MainActor
    func remove(identifier: UUID) async throws {
        try await WKWebsiteDataStore.remove(forIdentifier: identifier)
    }

    /// `allDataStoreIdentifiers` doesn't start WebKit. As the first WebKit call of the process, as in
    /// a background launch that only fetches feeds, it completes on a main run loop WebKit never set
    /// up and crashes. Creating any data store starts WebKit.
    @MainActor
    static func allIdentifiers() async -> [UUID] {
        if !webKitStarted {
            _ = WKWebsiteDataStore.nonPersistent()
            webKitStarted = true
        }
        return await WKWebsiteDataStore.allDataStoreIdentifiers
    }
}
