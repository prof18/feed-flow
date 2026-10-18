import WebKit
import XCTest

@MainActor
final class FeedRecoveryBrowserProfileTests: XCTestCase {
    func testProfilesAreIsolatedAndDefaultStoreDoesNotSeeProfileCookie() async throws {
        let defaultHadCookieBefore = await FeedRecoveryBrowserProfile.defaultStoreContainsFixtureCookie()
        let first = try FeedRecoveryBrowserProfile(identifier: UUID())
        let second = try FeedRecoveryBrowserProfile(identifier: UUID())

        XCTAssertTrue(first.hasIsolatedWebsiteDataStore)
        try await first.setFixtureCookie()

        let firstHasCookie = await first.containsFixtureCookie()
        let secondHasCookie = await second.containsFixtureCookie()
        let defaultHadCookieAfter = await FeedRecoveryBrowserProfile.defaultStoreContainsFixtureCookie()
        XCTAssertTrue(firstHasCookie)
        XCTAssertFalse(secondHasCookie)
        XCTAssertEqual(defaultHadCookieAfter, defaultHadCookieBefore)
        await first.clearAllWebsiteData()
        await second.clearAllWebsiteData()
        first.close()
        second.close()
    }

    func testCookieClearOnlyAffectsItsProfile() async throws {
        let first = try FeedRecoveryBrowserProfile(identifier: UUID())
        let control = try FeedRecoveryBrowserProfile(identifier: UUID())
        try await first.setFixtureCookie()
        try await control.setFixtureCookie()

        await first.clearCookies()

        let firstHasCookie = await first.containsFixtureCookie()
        let controlHasCookie = await control.containsFixtureCookie()
        XCTAssertFalse(firstHasCookie)
        XCTAssertTrue(controlHasCookie)
        await control.clearAllWebsiteData()
        first.close()
        control.close()
    }

    func testAllWebsiteDataClearOnlyAffectsItsProfile() async throws {
        let first = try FeedRecoveryBrowserProfile(identifier: UUID())
        let control = try FeedRecoveryBrowserProfile(identifier: UUID())
        try await first.setFixtureCookie()
        try await control.setFixtureCookie()

        await first.clearAllWebsiteData()

        let firstHasCookie = await first.containsFixtureCookie()
        let controlHasCookie = await control.containsFixtureCookie()
        XCTAssertFalse(firstHasCookie)
        XCTAssertTrue(controlHasCookie)
        await control.clearAllWebsiteData()
        first.close()
        control.close()
    }

    func testPersistentIdentifierPreservesCookieAcrossReopenAndClearRemovesIt() async throws {
        let identifier = UUID()
        let first = try FeedRecoveryBrowserProfile(identifier: identifier)
        try await first.setFixtureCookie()
        first.close()

        let reopened = try FeedRecoveryBrowserProfile(identifier: identifier)
        let reopenedHasCookie = await reopened.containsFixtureCookie()
        XCTAssertTrue(reopenedHasCookie)
        await reopened.clearAllWebsiteData()
        reopened.close()

        let cleared = try FeedRecoveryBrowserProfile(identifier: identifier)
        let clearedHasCookie = await cleared.containsFixtureCookie()
        XCTAssertFalse(clearedHasCookie)
        await cleared.clearAllWebsiteData()
        cleared.close()
    }
}
