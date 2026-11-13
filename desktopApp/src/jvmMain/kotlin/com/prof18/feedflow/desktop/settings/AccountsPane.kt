package com.prof18.feedflow.desktop.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.prof18.feedflow.desktop.accounts.AccountDrafts
import com.prof18.feedflow.desktop.accounts.AccountsScreen

@Composable
@NonRestartableComposable
internal fun AccountsPane(
    drafts: AccountDrafts,
    onBack: () -> Unit,
    backLabel: String,
) {
    AccountsScreen(drafts = drafts, onBack = onBack, backLabel = backLabel)
}
