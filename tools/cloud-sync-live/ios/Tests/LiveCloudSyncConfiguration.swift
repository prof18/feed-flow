import Foundation

struct LiveCloudSyncConfiguration: Decodable {
    let runID: String
    let appBundleID: String
    let timeoutSeconds: TimeInterval

    static func load(from bundle: Bundle) throws -> Self {
        guard let url = bundle.url(forResource: "Configuration", withExtension: "json") else {
            throw LiveCloudSyncError.invalidConfiguration("Configuration.json is missing from the UI-test bundle.")
        }
        let configuration = try JSONDecoder().decode(Self.self, from: Data(contentsOf: url))
        guard UUID(uuidString: configuration.runID) != nil else {
            throw LiveCloudSyncError.invalidConfiguration("Configuration.json runID must be a UUID.")
        }
        guard !configuration.appBundleID.isEmpty else {
            throw LiveCloudSyncError.invalidConfiguration("Configuration.json appBundleID must be non-empty.")
        }
        guard configuration.timeoutSeconds > 0 else {
            throw LiveCloudSyncError.invalidConfiguration("Configuration.json timeoutSeconds must be positive.")
        }
        return configuration
    }
}
