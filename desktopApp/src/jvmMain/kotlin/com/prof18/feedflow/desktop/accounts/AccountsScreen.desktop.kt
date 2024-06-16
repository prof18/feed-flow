package com.prof18.feedflow.desktop.accounts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.shared.presentation.AccountsViewModel
import com.prof18.feedflow.shared.ui.accounts.AccountsContent

internal class AccountsScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<AccountsViewModel>() }
        val accountSync by viewModel.accountsState.collectAsState()

        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            viewModel.restoreAccounts()
        }

        AccountsContent(
            syncAccount = accountSync,
            onDropboxCLick = {
                navigator.push(DropboxSyncScreen())
            },
            onBackClick = {
                navigator.pop()
            },
        )
    }
}
