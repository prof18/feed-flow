package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountsScreen(
    navigateBack: () -> Unit,
    navigateToDropboxSync: () -> Unit,
    navigateToGoogleDriveSync: () -> Unit,
    navigateToICloudSync: () -> Unit,
    navigateToFreshRssSync: () -> Unit,
    navigateToMinifluxSync: () -> Unit,
    navigateToBazquxSync: () -> Unit,
    navigateToFeedbinSync: () -> Unit,
) {
    val viewModel = koinViewModel<AccountsViewModel>()
    val accountSync by viewModel.accountsState.collectAsState()

    AccountsContent(
        syncAccount = accountSync,
        accounts = viewModel.getSupportedAccounts().toPersistentList(),
        onDropboxCLick = navigateToDropboxSync,
        onGoogleDriveClick = navigateToGoogleDriveSync,
        onICloudClick = navigateToICloudSync,
        onBackClick = navigateBack,
        onFreshRssClick = navigateToFreshRssSync,
        onMinifluxClick = navigateToMinifluxSync,
        onBazquxClick = navigateToBazquxSync,
        onFeedbinClick = navigateToFeedbinSync,
    )
}
