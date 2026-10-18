import Foundation

/// The WebKit data-store calls the profile store needs, behind a seam so tests can count them.
protocol WebsiteDataStoreProviding {
    @MainActor
    func identifiers() async -> [UUID]
    @MainActor
    func remove(identifier: UUID) async throws
}
