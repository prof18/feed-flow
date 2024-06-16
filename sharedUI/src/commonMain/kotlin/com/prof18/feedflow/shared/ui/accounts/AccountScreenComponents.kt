package com.prof18.feedflow.shared.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.SyncAccounts
import com.prof18.feedflow.shared.ui.accounts.icons.Dropbox
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun AccountsContent(
    syncAccount: SyncAccounts,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDropboxCLick: () -> Unit,
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountsItem(
                    modifier = Modifier
                        .padding(top = Spacing.medium)
                        .weight(1f),
                    title = "Dropbox",
                    icon = Dropbox,
                    onClick = onDropboxCLick,
                )

                if (syncAccount == SyncAccounts.DROPBOX) {
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

@Composable
private fun AccountsItem(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick() }
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
