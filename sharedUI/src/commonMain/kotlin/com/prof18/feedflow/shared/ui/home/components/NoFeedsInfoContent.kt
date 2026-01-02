package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun NoFeedsInfoContent(
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onAccountsClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onFeedSuggestionsClick: () -> Unit = {},
) {
    Column(
        modifier = modifier,
    ) {
        if (showTitle) {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular),
                text = LocalFeedFlowStrings.current.noFeedModalTitle,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            modifier = Modifier
                .padding(vertical = Spacing.regular)
                .padding(horizontal = Spacing.regular),
            text = LocalFeedFlowStrings.current.noFeedModalMessage,
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular),
            onClick = {
                onDismissRequest()
                onAddFeedClick()
            },
        ) {
            Text(LocalFeedFlowStrings.current.addFeed)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.regular)
                .padding(horizontal = Spacing.regular),
            onClick = {
                onDismissRequest()
                onImportExportClick()
            },
        ) {
            Text(LocalFeedFlowStrings.current.importExportLabel)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.regular)
                .padding(horizontal = Spacing.regular),
            onClick = {
                onDismissRequest()
                onAccountsClick()
            },
        ) {
            Text(LocalFeedFlowStrings.current.settingsAccounts)
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.regular)
                .padding(bottom = Spacing.medium)
                .padding(horizontal = Spacing.regular),
            onClick = {
                onDismissRequest()
                onFeedSuggestionsClick()
            },
        ) {
            Text(LocalFeedFlowStrings.current.feedSuggestionsTitle)
        }
    }
}
