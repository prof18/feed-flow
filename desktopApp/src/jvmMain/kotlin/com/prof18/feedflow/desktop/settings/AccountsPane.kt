package com.prof18.feedflow.desktop.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.desktop.accounts.AccountsScreen
import com.prof18.feedflow.desktop.accounts.bazqux.BazquxSyncScreen
import com.prof18.feedflow.desktop.accounts.dropbox.DropboxSyncScreen
import com.prof18.feedflow.desktop.accounts.feedbin.FeedbinSyncScreen
import com.prof18.feedflow.desktop.accounts.freshrss.FreshRssSyncScreen
import com.prof18.feedflow.desktop.accounts.googledrive.GoogleDriveSyncScreen
import com.prof18.feedflow.desktop.accounts.icloud.ICloudSyncScreen
import com.prof18.feedflow.desktop.accounts.miniflux.MinifluxSyncScreen

@Composable
internal fun AccountsPane() {
    var selectedAccount: SyncAccounts? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedAccount) {
            null -> AccountsScreen(
                selectedAccount = null,
                navigateToDropboxSync = { selectedAccount = SyncAccounts.DROPBOX },
                navigateToGoogleDriveSync = { selectedAccount = SyncAccounts.GOOGLE_DRIVE },
                navigateToICloudSync = { selectedAccount = SyncAccounts.ICLOUD },
                navigateToFreshRssSync = { selectedAccount = SyncAccounts.FRESH_RSS },
                navigateToMinifluxSync = { selectedAccount = SyncAccounts.MINIFLUX },
                navigateToBazquxSync = { selectedAccount = SyncAccounts.BAZQUX },
                navigateToFeedbinSync = { selectedAccount = SyncAccounts.FEEDBIN },
            )

            SyncAccounts.DROPBOX -> DropboxSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.GOOGLE_DRIVE -> GoogleDriveSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.ICLOUD -> ICloudSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.FRESH_RSS -> FreshRssSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.MINIFLUX -> MinifluxSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.BAZQUX -> BazquxSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            SyncAccounts.FEEDBIN -> FeedbinSyncScreen(
                navigateBack = { selectedAccount = null },
                showNavigateBack = true,
            )

            else -> {}
        }
    }
}
