import GoogleAPIClientForREST_Drive
import XCTest

final class GoogleDriveScopeTests: XCTestCase {
    func testGrantedDriveAppdataScopeHasDriveAccess() {
        let scopes = [
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            kGTLRAuthScopeDriveAppdata,
        ]

        XCTAssertTrue(GoogleDriveScope.hasDriveAccess(grantedScopes: scopes))
    }

    func testSignInWithoutTheDriveCheckboxHasNoDriveAccess() {
        let scopes = [
            "openid",
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile",
        ]

        XCTAssertFalse(GoogleDriveScope.hasDriveAccess(grantedScopes: scopes))
    }

    func testMissingGrantedScopesHasNoDriveAccess() {
        XCTAssertFalse(GoogleDriveScope.hasDriveAccess(grantedScopes: nil))
        XCTAssertFalse(GoogleDriveScope.hasDriveAccess(grantedScopes: []))
    }

    func testRequiredScopesAskForDriveAppdata() {
        XCTAssertEqual(GoogleDriveScope.required, [kGTLRAuthScopeDriveAppdata])
    }
}
