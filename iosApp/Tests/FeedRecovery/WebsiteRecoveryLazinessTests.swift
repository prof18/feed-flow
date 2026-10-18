import Foundation
import XCTest

@MainActor
final class WebsiteRecoveryLazinessTests: XCTestCase {
    func testCreatingTheBridgeAndStoreMakesNoDataStoreCall() {
        let provider = CountingDataStoreProvider()
        let records = FakeProfileRecords()

        _ = WebsiteSessionBridge()
        _ = WebsiteProfileStore(provider: provider, records: records)

        XCTAssertEqual(provider.calls, 0)
        XCTAssertEqual(records.calls, 0)
    }

    func testCleanWithoutProfilesMakesNoDataStoreCall() async throws {
        let provider = CountingDataStoreProvider()
        let store = WebsiteProfileStore(provider: provider, records: FakeProfileRecords())

        try await store.removeRetiredProfiles()

        XCTAssertEqual(provider.calls, 0)
    }

    func testCleanRemovesOneRetiredProfileOnce() async throws {
        let provider = CountingDataStoreProvider()
        let identifier = UUID()
        let records = FakeProfileRecords(retired: ["feedflow-recovery-\(identifier.uuidString)"])
        let store = WebsiteProfileStore(provider: provider, records: records)

        try await store.removeRetiredProfiles()

        XCTAssertEqual(provider.removed, [identifier])
        XCTAssertEqual(records.deletionCompleted, records.retiredIds)
    }

    func testFailedRemovalKeepsThatProfileAndContinuesWithTheOthers() async throws {
        let busy = UUID()
        let free = UUID()
        let provider = CountingDataStoreProvider(failing: [busy])
        let busyId = "feedflow-recovery-\(busy.uuidString)"
        let freeId = "feedflow-recovery-\(free.uuidString)"
        let records = FakeProfileRecords(retired: [busyId, freeId])
        let store = WebsiteProfileStore(provider: provider, records: records)

        try await store.removeRetiredProfiles()

        XCTAssertEqual(provider.removed, [free])
        XCTAssertEqual(records.deletionCompleted, [freeId])
    }

    func testCleanRetiresActiveProfileMissingFromWebKit() async throws {
        let present = UUID()
        let provider = CountingDataStoreProvider(identifiers: [present])
        let missingId = "feedflow-recovery-\(UUID().uuidString)"
        let presentId = "feedflow-recovery-\(present.uuidString)"
        let records = FakeProfileRecords(active: [missingId, presentId])
        let store = WebsiteProfileStore(provider: provider, records: records)

        try await store.removeRetiredProfiles()

        XCTAssertEqual(records.cleared, [missingId])
    }
}

@MainActor
private final class CountingDataStoreProvider: WebsiteDataStoreProviding {
    private let storedIdentifiers: [UUID]
    private let failing: Set<UUID>
    private(set) var calls = 0
    private(set) var removed: [UUID] = []

    init(identifiers: [UUID] = [], failing: Set<UUID> = []) {
        storedIdentifiers = identifiers
        self.failing = failing
    }

    func identifiers() async -> [UUID] {
        calls += 1
        return storedIdentifiers
    }

    func remove(identifier: UUID) async throws {
        calls += 1
        if failing.contains(identifier) {
            throw URLError(.cannotRemoveFile)
        }
        removed.append(identifier)
    }
}

@MainActor
private final class FakeProfileRecords: WebsiteProfileRecords {
    private(set) var activeIds: [String]
    private(set) var retiredIds: [String]
    private(set) var calls = 0
    private(set) var cleared: [String] = []
    private(set) var deletionCompleted: [String] = []

    init(active: [String] = [], retired: [String] = []) {
        activeIds = active
        retiredIds = retired
    }

    func hasProfiles() async throws -> Bool {
        calls += 1
        return !activeIds.isEmpty || !retiredIds.isEmpty
    }

    func retireOtherEpochs() async throws {
        calls += 1
    }

    func activeProfileIds() async throws -> [String] {
        calls += 1
        return activeIds
    }

    func retiredProfileIds() async throws -> [String] {
        calls += 1
        return retiredIds.filter { profileId in !deletionCompleted.contains(profileId) }
    }

    func retireMissingProfile(profileId: String) async throws {
        calls += 1
        cleared.append(profileId)
        activeIds.removeAll { $0 == profileId }
    }

    func nativeDeletionCompleted(profileId: String) async throws {
        calls += 1
        deletionCompleted.append(profileId)
    }
}
