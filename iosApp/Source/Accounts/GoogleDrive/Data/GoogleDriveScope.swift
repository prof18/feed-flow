import GoogleAPIClientForREST_Drive

/// Google's consent screen shows the Drive permission as an optional checkbox, so a successful
/// sign-in does not mean the app can reach Drive.
enum GoogleDriveScope {
    static let required = [kGTLRAuthScopeDriveAppdata]

    static func hasDriveAccess(grantedScopes: [String]?) -> Bool {
        guard let grantedScopes else { return false }
        return required.allSatisfy(grantedScopes.contains)
    }
}
