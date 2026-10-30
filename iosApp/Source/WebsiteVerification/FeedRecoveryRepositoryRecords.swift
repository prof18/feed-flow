import FeedFlowKit
import Foundation

/// Reads the repository on each call, so creating it does not touch Koin or the database.
struct FeedRecoveryRepositoryRecords: WebsiteProfileRecords {
    private var repository: FeedRecoveryRepository {
        Deps.shared.getFeedRecoveryRepository()
    }

    @MainActor
    func hasProfiles() async throws -> Bool {
        try await repository.hasProfiles().boolValue
    }

    @MainActor
    func retireOtherEpochs() async throws {
        _ = try await repository.housekeeping(epoch: repository.accountEpoch)
    }

    @MainActor
    func activeProfileIds() async throws -> [String] {
        let epoch = repository.accountEpoch
        return try await repository.profiles()
            .filter { profile in !profile.retired && profile.accountEpoch == epoch }
            .map(\.id)
    }

    @MainActor
    func retiredProfileIds() async throws -> [String] {
        try await repository.profiles().filter(\.retired).map(\.id)
    }

    @MainActor
    func retireMissingProfile(profileId: String) async throws {
        try await repository.retireMissingProfile(profileId: profileId)
    }

    @MainActor
    func nativeDeletionCompleted(profileId: String) async throws {
        try await repository.nativeDeletionCompleted(profileId: profileId)
    }
}
