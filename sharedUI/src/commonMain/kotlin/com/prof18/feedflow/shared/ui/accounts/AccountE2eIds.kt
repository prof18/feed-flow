package com.prof18.feedflow.shared.ui.accounts

import com.prof18.feedflow.core.model.SyncAccounts

object AccountE2eIds {
    const val SERVER_URL_INPUT = "account_server_url_input"
    const val USERNAME_INPUT = "account_username_input"
    const val PASSWORD_INPUT = "account_password_input"
    const val PASSWORD_VISIBILITY = "account_password_visibility"
    const val CONNECT_BUTTON = "account_connect_button"
    const val CONNECTED_MESSAGE = "account_connected_message"
    const val LAST_SYNC_LABEL = "account_last_sync_label"
    const val DISCONNECT_BUTTON = "account_disconnect_button"

    fun providerRow(account: SyncAccounts): String =
        "accounts_provider_${account.name.lowercase()}"
}
