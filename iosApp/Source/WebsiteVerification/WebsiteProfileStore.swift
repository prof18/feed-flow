import FeedFlowKit
import Foundation
import WebKit

/// Deletes the WebKit data of retired recovery profiles. Creating it does no work, and users who
/// never verified a website have no profiles, so it never touches WebKit for them.
final class WebsiteProfileStore: WebsiteProfileCleaner {
    static let shared = WebsiteProfileStore(
        provider: WebKitWebsiteDataStoreProvider(),
        records: FeedRecoveryRepositoryRecords()
    )
    static let engineVersion = ProcessInfo.processInfo.operatingSystemVersionString

    private let provider: any WebsiteDataStoreProviding
    private let records: any WebsiteProfileRecords
    @MainActor private var cachedUserAgent: String?

    init(provider: any WebsiteDataStoreProviding, records: any WebsiteProfileRecords) {
        self.provider = provider
        self.records = records
    }

    func clean(completionHandler: @escaping (Error?) -> Void) {
        Task { @MainActor in
            do {
                try await removeRetiredProfiles()
                completionHandler(nil)
            } catch {
                completionHandler(error)
            }
        }
    }

    /// Profiles are never retired because `engineVersion` changed: every iOS update would force a
    /// new verification.
    @MainActor
    func removeRetiredProfiles() async throws {
        guard try await records.hasProfiles() else { return }
        try await records.retireOtherEpochs()
        let identifiers = Set(await provider.identifiers())
        for profileId in try await records.activeProfileIds() {
            let identifier = WebsiteProfileIdentity.identifier(profileId: profileId)
            if identifier.map({ identifiers.contains($0) }) != true {
                try await records.retireMissingProfile(profileId: profileId)
            }
        }
        for profileId in try await records.retiredProfileIds() {
            if let identifier = WebsiteProfileIdentity.identifier(profileId: profileId) {
                // WebKit refuses to remove a store a web view still uses; the next clean retries it.
                do { try await provider.remove(identifier: identifier) } catch { continue }
            }
            try await records.nativeDeletionCompleted(profileId: profileId)
        }
    }

    @MainActor
    func userAgent() async throws -> String {
        if let cachedUserAgent {
            return cachedUserAgent
        }
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        let browser = WKWebView(frame: .zero, configuration: configuration)
        defer { browser.stopLoading() }
        guard let value = try await browser.evaluateJavaScript("navigator.userAgent") as? String,
              !value.isEmpty else { throw URLError(.unsupportedURL) }
        cachedUserAgent = value
        return value
    }
}
