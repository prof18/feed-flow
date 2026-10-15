package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import com.prof18.feedflow.desktop.accounts.AccountDrafts
import com.prof18.feedflow.desktop.accounts.AccountsScreen

@Composable
internal fun AccountsPane(drafts: AccountDrafts) {
    AccountsScreen(drafts = drafts)
}
