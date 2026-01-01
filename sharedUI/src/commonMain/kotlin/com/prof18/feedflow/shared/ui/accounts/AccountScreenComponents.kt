package com.prof18.feedflow.shared.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.shared.ui.accounts.icons.Cloud
import com.prof18.feedflow.shared.ui.accounts.icons.Dropbox
import com.prof18.feedflow.shared.ui.accounts.icons.Feedbin
import com.prof18.feedflow.shared.ui.accounts.icons.FreshRSS
import com.prof18.feedflow.shared.ui.accounts.icons.GoogleDriveLogo
import com.prof18.feedflow.shared.ui.accounts.icons.Miniflux
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AccountsContent(
    syncAccount: SyncAccounts,
    accounts: ImmutableList<SyncAccounts>,
    onBackClick: () -> Unit,
    onDropboxCLick: () -> Unit,
    onGoogleDriveClick: () -> Unit,
    onICloudClick: () -> Unit,
    onFreshRssClick: () -> Unit,
    onMinifluxClick: () -> Unit,
    onFeedbinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = {
                    Row {
                        Text(text = LocalFeedFlowStrings.current.settingsAccounts)
                        Text(
                            text = "BETA",
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding),
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular),
                text = LocalFeedFlowStrings.current.accountsDescription,
                style = MaterialTheme.typography.bodyMedium,
            )

            LazyColumn(
                modifier = Modifier
                    .padding(top = Spacing.medium),
            ) {
                items(accounts) { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val isEnabled = syncAccount == SyncAccounts.LOCAL || syncAccount == account
                        AccountsItem(
                            modifier = Modifier.weight(1f)
                                .alpha(alpha = if (isEnabled) 1f else 0.5f),
                            title = account.getTitle(),
                            icon = account.getIcon(),
                            isClickEnabled = isEnabled,
                            onClick = when (account) {
                                SyncAccounts.DROPBOX -> onDropboxCLick
                                SyncAccounts.GOOGLE_DRIVE -> onGoogleDriveClick
                                SyncAccounts.ICLOUD -> onICloudClick
                                SyncAccounts.LOCAL -> {
                                    { }
                                }
                                SyncAccounts.FRESH_RSS -> onFreshRssClick
                                SyncAccounts.MINIFLUX -> onMinifluxClick
                                SyncAccounts.FEEDBIN -> onFeedbinClick
                            },
                        )

                        if (syncAccount == account) {
                            Icon(
                                modifier = Modifier
                                    .padding(top = Spacing.medium)
                                    .padding(horizontal = Spacing.regular),
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SyncAccounts.getTitle() =
    when (this) {
        SyncAccounts.DROPBOX -> "Dropbox"
        SyncAccounts.GOOGLE_DRIVE -> "Google Drive"
        SyncAccounts.LOCAL -> "Local"
        SyncAccounts.ICLOUD -> "iCloud"
        SyncAccounts.FRESH_RSS -> "FreshRSS"
        SyncAccounts.MINIFLUX -> "Miniflux"
        SyncAccounts.FEEDBIN -> "Feedbin"
        SyncAccounts.MINIFLUX -> "Miniflux"
        SyncAccounts.FEEDBIN -> "Feedbin"
    }

private fun SyncAccounts.getIcon() =
    when (this) {
        SyncAccounts.DROPBOX -> Dropbox
        SyncAccounts.GOOGLE_DRIVE -> GoogleDriveLogo
        SyncAccounts.ICLOUD -> Cloud
        SyncAccounts.LOCAL -> Dropbox
        SyncAccounts.FRESH_RSS -> FreshRSS
        SyncAccounts.MINIFLUX -> Miniflux
        SyncAccounts.FEEDBIN -> Feedbin
        SyncAccounts.MINIFLUX -> Miniflux
        SyncAccounts.FEEDBIN -> Feedbin
    }

@Composable
private fun AccountsItem(
    title: String,
    icon: ImageVector,
    isClickEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(
                enabled = isClickEnabled,
                onClick = { onClick() },
            )
            .padding(Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            icon,
            contentDescription = null,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
