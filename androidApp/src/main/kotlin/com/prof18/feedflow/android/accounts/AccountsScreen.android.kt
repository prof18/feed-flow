package com.prof18.feedflow.android.accounts

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            Lifecycle.State.RESUMED -> {
                viewModel.restoreAccounts()
            }
            else -> {}
        }
    }

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
