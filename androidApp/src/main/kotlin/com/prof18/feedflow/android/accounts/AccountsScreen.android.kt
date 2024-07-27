package com.prof18.feedflow.android.accounts

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.accounts.dropbox.DropboxSyncActivity
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun AccountsScreen(
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<AccountsViewModel>()

    val syncAccount by viewModel.accountsState.collectAsStateWithLifecycle()

    AccountsContent(
        syncAccount = syncAccount,
        onBackClick = navigateBack,
        onDropboxCLick = {
            context.startActivity(
                Intent(context, DropboxSyncActivity::class.java),
            )
        },
    )
}
