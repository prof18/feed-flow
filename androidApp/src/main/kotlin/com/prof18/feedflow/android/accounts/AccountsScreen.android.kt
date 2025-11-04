package com.prof18.feedflow.android.accounts

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.accounts.dropbox.DropboxSyncActivity
import com.prof18.feedflow.android.accounts.googledrive.GoogleDriveSyncActivity
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountsScreen(
    navigateBack: () -> Unit,
    navigateToFreshRssSync: () -> Unit,
    navigateToMinifluxSync: () -> Unit,
    navigateToFeedbinSync: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<AccountsViewModel>()

    val syncAccount by viewModel.accountsState.collectAsStateWithLifecycle()

    AccountsContent(
        syncAccount = syncAccount,
        accounts = viewModel.getSupportedAccounts().toPersistentList(),
        onBackClick = navigateBack,
        onDropboxCLick = {
            context.startActivity(
                Intent(context, DropboxSyncActivity::class.java),
            )
        },
        onGoogleDriveClick = {
            context.startActivity(
                Intent(context, GoogleDriveSyncActivity::class.java),
            )
        },
        onICloudClick = {},
        onFreshRssClick = navigateToFreshRssSync,
        onMinifluxClick = navigateToMinifluxSync,
        onFeedbinClick = navigateToFeedbinSync,
        onMinifluxClick = navigateToMinifluxSync,
        onFeedbinClick = navigateToFeedbinSync,
    )
}
