package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent
import kotlinx.collections.immutable.toPersistentList

internal class AccountsScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<AccountsViewModel>() }
        val accountSync by viewModel.accountsState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow

        AccountsContent(
            syncAccount = accountSync,
            accounts = viewModel.getSupportedAccounts().toPersistentList(),
            onDropboxCLick = {
                navigator.push(DropboxSyncScreen())
            },
            onICloudClick = {
                navigator.push(ICloudSyncScreen())
            },
            onBackClick = {
                navigator.pop()
            },
        )
    }
}
