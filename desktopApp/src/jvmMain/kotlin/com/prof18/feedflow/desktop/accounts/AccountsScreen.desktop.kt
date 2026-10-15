package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountsScreen(modifier: Modifier = Modifier, drafts: AccountDrafts = rememberAccountDrafts()) {
    val viewModel = koinViewModel<AccountsViewModel>()
    val currentAccount by viewModel.accountsState.collectAsState()
    val supportedAccounts = remember(viewModel) { viewModel.getSupportedAccounts().toImmutableList() }
    DesktopAccountsContent(
        currentAccount = currentAccount,
        supportedAccounts = supportedAccounts,
        modifier = modifier.fillMaxSize(),
        drafts = drafts,
    ) { account, _, _, credentials ->
        DesktopAccountDetail(account, credentials)
    }
}

@Composable
private fun DesktopAccountDetail(account: SyncAccounts, credentials: CredentialsState) {
    when (account) {
        SyncAccounts.DROPBOX -> DropboxSyncScreen(credentials)
        SyncAccounts.GOOGLE_DRIVE -> GoogleDriveSyncScreen(credentials)
        SyncAccounts.ICLOUD -> ICloudSyncScreen(credentials)
        SyncAccounts.FRESH_RSS -> FreshRssSyncScreen(credentials)
        SyncAccounts.MINIFLUX -> MinifluxSyncScreen(credentials)
        SyncAccounts.BAZQUX -> BazquxSyncScreen(credentials)
        SyncAccounts.FEEDBIN -> FeedbinSyncScreen(credentials)
        SyncAccounts.LOCAL -> Unit
    }
}
