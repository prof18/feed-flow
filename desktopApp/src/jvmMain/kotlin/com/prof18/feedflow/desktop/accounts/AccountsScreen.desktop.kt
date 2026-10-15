package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.DesktopDialogWindow
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DesktopAccountsWindow(onCloseRequest: () -> Unit) {
    DesktopDialogWindow(
        title = LocalFeedFlowStrings.current.settingsAccounts,
        size = DpSize(width = 960.dp, height = 720.dp),
        minimumSize = DpSize(width = 680.dp, height = 520.dp),
        onCloseRequest = onCloseRequest,
    ) { modifier ->
        AccountsScreen(modifier.padding(16.dp))
    }
}

@Composable
internal fun AccountsScreen(modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<AccountsViewModel>()
    val currentAccount by viewModel.accountsState.collectAsState()
    val supportedAccounts = remember(viewModel) { viewModel.getSupportedAccounts().toImmutableList() }
    DesktopAccountsContent(
        currentAccount = currentAccount,
        supportedAccounts = supportedAccounts,
        modifier = modifier.fillMaxSize(),
    ) { account, navigateBack, showBack, credentials ->
        DesktopAccountDetail(account, navigateBack, showBack, credentials)
    }
}

@Composable
private fun DesktopAccountDetail(
    account: SyncAccounts,
    navigateBack: () -> Unit,
    showBack: Boolean,
    credentials: CredentialsState,
) {
    when (account) {
        SyncAccounts.DROPBOX -> DropboxSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.GOOGLE_DRIVE -> GoogleDriveSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.ICLOUD -> ICloudSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.FRESH_RSS -> FreshRssSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.MINIFLUX -> MinifluxSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.BAZQUX -> BazquxSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.FEEDBIN -> FeedbinSyncScreen(navigateBack, showBack, credentials)
        SyncAccounts.LOCAL -> Unit
    }
}
