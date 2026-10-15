package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.prof18.feedflow.core.model.SyncAccounts
import kotlinx.coroutines.CoroutineScope

internal class AccountDrafts(private val scope: CoroutineScope) {
    private val drafts = mutableMapOf<SyncAccounts, CredentialsState>()

    fun forAccount(account: SyncAccounts): CredentialsState = drafts.getOrPut(account) { CredentialsState(scope) }
}

@Composable
internal fun rememberAccountDrafts(): AccountDrafts {
    val scope = rememberCoroutineScope()
    return remember(scope) { AccountDrafts(scope) }
}
