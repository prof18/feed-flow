enum AccountAccessibilityIdentifiers {
    static let addAccount = "accounts_add_account"
    static let serverUrlInput = "account_server_url_input"
    static let usernameInput = "account_username_input"
    static let passwordInput = "account_password_input"
    static let passwordVisibility = "account_password_visibility"
    static let connectButton = "account_connect_button"

    static func provider(_ id: String) -> String {
        "accounts_provider_\(id)"
    }
}
