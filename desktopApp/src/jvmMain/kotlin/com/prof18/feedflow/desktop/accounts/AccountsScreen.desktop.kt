package com.prof18.feedflow.desktop.accounts

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import com.prof18.feedflow.shared.ui.style.Spacing
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountsScreen(
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
        contentPadding = PaddingValues(top = Spacing.regular),
        onDropboxCLick = navigateToDropboxSync,
        onGoogleDriveClick = navigateToGoogleDriveSync,
        onICloudClick = navigateToICloudSync,
        onFreshRssClick = navigateToFreshRssSync,
        onMinifluxClick = navigateToMinifluxSync,
        onBazquxClick = navigateToBazquxSync,
        onFeedbinClick = navigateToFeedbinSync,
    )
}
