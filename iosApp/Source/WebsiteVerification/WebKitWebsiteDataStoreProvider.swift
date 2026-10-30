import Foundation
import WebKit

struct WebKitWebsiteDataStoreProvider: WebsiteDataStoreProviding {
    @MainActor
    func identifiers() async -> [UUID] {
        await WKWebsiteDataStore.allDataStoreIdentifiers
    }

    @MainActor
    func remove(identifier: UUID) async throws {
        try await WKWebsiteDataStore.remove(forIdentifier: identifier)
    }
}
