package com.prof18.feedflow.android.accounts

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.android.accounts.dropbox.DropboxSyncActivity
import com.prof18.feedflow.android.accounts.googledrive.GoogleDriveSyncActivity
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.toPersistentList
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AccountsScreen(
    navigateBack: () -> Unit,
    navigateToFreshRssSync: () -> Unit,
    navigateToMinifluxSync: () -> Unit,
    navigateToBazquxSync: () -> Unit,
    navigateToFeedbinSync: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = koinViewModel<AccountsViewModel>()

    val syncAccount by viewModel.accountsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Text(text = LocalFeedFlowStrings.current.settingsAccounts)
                },
            )
        },
    ) { innerPadding ->
        AccountsContent(
            syncAccount = syncAccount,
            accounts = viewModel.getSupportedAccounts().toPersistentList(),
            contentPadding = innerPadding,
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
            onBazquxClick = navigateToBazquxSync,
            onFeedbinClick = navigateToFeedbinSync,
        )
    }
}
