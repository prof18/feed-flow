import Foundation

/// The recovery-profile rows the profile store reads and updates, behind a seam so tests run without Koin.
protocol WebsiteProfileRecords {
    @MainActor
    func hasProfiles() async throws -> Bool
    @MainActor
    func retireOtherEpochs() async throws
    @MainActor
    func activeProfileIds() async throws -> [String]
    @MainActor
    func retiredProfileIds() async throws -> [String]
    @MainActor
    func retireMissingProfile(profileId: String) async throws
    @MainActor
    func nativeDeletionCompleted(profileId: String) async throws
}
