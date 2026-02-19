package com.prof18.feedflow.shared.ui.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ImportExportContent(
    feedImportExportState: FeedImportExportState,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportArticlesClick: () -> Unit,
    onExportArticlesClick: (ArticleExportFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showExportArticlesDialog by remember { mutableStateOf(false) }
    var articleExportFilter by remember { mutableStateOf(ArticleExportFilter.All) }

    if (showExportArticlesDialog) {
        ArticleExportDialog(
            selectedFilter = articleExportFilter,
            onFilterSelected = { articleExportFilter = it },
            onDismiss = { showExportArticlesDialog = false },
            onConfirm = {
                showExportArticlesDialog = false
                onExportArticlesClick(articleExportFilter)
            },
        )
    }

    ImportExportBody(
        feedImportExportState = feedImportExportState,
        onRetryClick = onRetryClick,
        onDoneClick = onDoneClick,
        onImportClick = onImportClick,
        onExportClick = onExportClick,
        onImportArticlesClick = onImportArticlesClick,
        onOpenExportArticlesDialog = { showExportArticlesDialog = true },
        modifier = modifier,
    )
}

@Composable
private fun ImportExportBody(
    feedImportExportState: FeedImportExportState,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportArticlesClick: () -> Unit,
    onOpenExportArticlesDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (feedImportExportState) {
        is FeedImportExportState.Idle ->
            ImportExportIdleView(
                modifier = modifier,
                onImportClick = onImportClick,
                onExportClick = onExportClick,
                onImportArticlesClick = onImportArticlesClick,
                onExportArticlesClick = onOpenExportArticlesDialog,
            )

        FeedImportExportState.Error ->
            ImportExportErrorView(
                modifier = modifier.fillMaxSize(),
                onRetryClick = onRetryClick,
            )

        is FeedImportExportState.LoadingImport ->
            ImportExportLoadingView(
                modifier = modifier.fillMaxSize(),
                message = when (feedImportExportState.contentType) {
                    ImportExportContentType.FeedsOpml ->
                        LocalFeedFlowStrings.current.feedAddInProgressMessage
                    ImportExportContentType.ArticlesCsv ->
                        LocalFeedFlowStrings.current.articlesImportingMessage
                },
            )

        is FeedImportExportState.LoadingExport ->
            ImportExportLoadingView(
                modifier = modifier.fillMaxSize(),
                message = LocalFeedFlowStrings.current.exportStartedMessage,
            )

        FeedImportExportState.ExportSuccess ->
            ExportDoneView(
                modifier = modifier.fillMaxSize(),
                onDoneClick = onDoneClick,
            )

        FeedImportExportState.ArticleExportSuccess ->
            ArticleExportDoneView(
                modifier = modifier.fillMaxSize(),
                onDoneClick = onDoneClick,
            )

        is FeedImportExportState.ImportSuccess ->
            ImportDoneView(
                modifier = modifier.fillMaxSize(),
                feedSourcesInvalid = feedImportExportState.notValidFeedSources,
                feedSourceWithError = feedImportExportState.feedSourceWithError,
                onDoneClick = onDoneClick,
            )

        FeedImportExportState.ArticleImportSuccess ->
            ArticleImportDoneView(
                modifier = modifier.fillMaxSize(),
                onDoneClick = onDoneClick,
            )
    }
}

@Composable
private fun ImportExportIdleView(
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportArticlesClick: () -> Unit,
    onExportArticlesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Text(
                modifier = Modifier
                    .padding(Spacing.regular),
                text = LocalFeedFlowStrings.current.importExportOpmlSectionTitle,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .padding(bottom = Spacing.regular),
                color = MaterialTheme.colorScheme.onBackground,
                text = LocalFeedFlowStrings.current.importExportDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.importFeedButton,
                icon = Icons.Default.FileDownload,
                onClick = onImportClick,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.exportFeedsButton,
                icon = Icons.Default.FileUpload,
                onClick = onExportClick,
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Spacing.regular),
            )
        }

        item {
            Text(
                modifier = Modifier
                    .padding(Spacing.regular),
                text = LocalFeedFlowStrings.current.importExportArticlesSectionTitle,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        item {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .padding(bottom = Spacing.regular),
                color = MaterialTheme.colorScheme.onBackground,
                text = LocalFeedFlowStrings.current.importExportArticlesDescription,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.importArticlesButton,
                icon = Icons.Default.FileDownload,
                onClick = onImportArticlesClick,
            )
        }

        item {
            SettingItem(
                title = LocalFeedFlowStrings.current.exportArticlesButton,
                icon = Icons.Default.FileUpload,
                onClick = onExportArticlesClick,
            )
        }
    }
}

@Composable
private fun ArticleExportDialog(
    selectedFilter: ArticleExportFilter,
    onFilterSelected: (ArticleExportFilter) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(LocalFeedFlowStrings.current.articlesExportFilterTitle)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.articlesExportFilterDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.small),
                )
                ArticleExportFilterSelector(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    showTitle = false,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(LocalFeedFlowStrings.current.confirmButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LocalFeedFlowStrings.current.cancelButton)
            }
        },
    )
}

@Composable
private fun ImportExportLoadingView(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()

        Text(
            modifier = Modifier
                .padding(Spacing.regular),
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImportExportErrorView(
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalFeedFlowStrings.current.genericErrorMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onRetryClick,
        ) {
            Text(LocalFeedFlowStrings.current.retryButton)
        }
    }
}

@Composable
private fun ExportDoneView(
    modifier: Modifier = Modifier,
    onDoneClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalFeedFlowStrings.current.feedsExportDoneMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onDoneClick,
        ) {
            Text(LocalFeedFlowStrings.current.doneButton)
        }
    }
}

@Composable
private fun ArticleExportDoneView(
    modifier: Modifier = Modifier,
    onDoneClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalFeedFlowStrings.current.articlesExportDoneMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onDoneClick,
        ) {
            Text(LocalFeedFlowStrings.current.doneButton)
        }
    }
}

@Composable
private fun ArticleImportDoneView(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = LocalFeedFlowStrings.current.articlesImportDoneMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onDoneClick,
        ) {
            Text(LocalFeedFlowStrings.current.doneButton)
        }
    }
}

@Composable
private fun ArticleExportFilterSelector(
    selectedFilter: ArticleExportFilter,
    onFilterSelected: (ArticleExportFilter) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xsmall),
    ) {
        if (showTitle) {
            Text(
                text = LocalFeedFlowStrings.current.articlesExportFilterTitle,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ArticleExportFilter.entries.forEach { filter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = filter == selectedFilter,
                        onClick = { onFilterSelected(filter) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = filter == selectedFilter,
                    onClick = null,
                )
                Text(
                    modifier = Modifier.padding(start = Spacing.small),
                    text = filter.toLabel(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ImportDoneView(
    feedSourcesInvalid: ImmutableList<ParsedFeedSource>,
    feedSourceWithError: ImmutableList<ParsedFeedSource>,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedSources = feedSourcesInvalid.ifEmpty { feedSourceWithError }
    val errorMessage = if (feedSourcesInvalid.isNotEmpty()) {
        LocalFeedFlowStrings.current.wrongLinkReportTitle
    } else {
        LocalFeedFlowStrings.current.linkWithErrorReportTitle
    }

    Column(
        modifier = modifier,
    ) {
        if (feedSources.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.feedsImportDoneMessage,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.regular),
                    onClick = onDoneClick,
                ) {
                    Text(LocalFeedFlowStrings.current.doneButton)
                }
            }
        } else {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .padding(top = Spacing.regular),
                text = errorMessage,
                style = MaterialTheme.typography.titleMedium,
            )

            FeedsNotAddedList(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = Spacing.regular),
                feedSources = feedSources,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.regular),
                onClick = onDoneClick,
            ) {
                Text(LocalFeedFlowStrings.current.doneButton)
            }
        }
    }
}

@Composable
private fun FeedsNotAddedList(
    feedSources: ImmutableList<ParsedFeedSource>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(feedSources) { feedSource ->
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(horizontal = Spacing.regular),
                    text = feedSource.title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    modifier = Modifier
                        .padding(top = Spacing.xsmall)
                        .padding(bottom = Spacing.small)
                        .padding(horizontal = Spacing.regular),
                    text = feedSource.url,
                    style = MaterialTheme.typography.labelLarge,
                )

                HorizontalDivider(
                    modifier = Modifier,
                    thickness = 0.2.dp,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun ArticleExportFilter.toLabel(): String =
    when (this) {
        ArticleExportFilter.All -> LocalFeedFlowStrings.current.articlesExportFilterAll
        ArticleExportFilter.Read -> LocalFeedFlowStrings.current.articlesExportFilterRead
        ArticleExportFilter.Unread -> LocalFeedFlowStrings.current.articlesExportFilterUnread
        ArticleExportFilter.Bookmarked -> LocalFeedFlowStrings.current.articlesExportFilterBookmarked
    }

@Preview
@Composable
private fun ImportExportIdlePreview() {
    PreviewTheme {
        ImportExportContent(
            feedImportExportState = FeedImportExportState.Idle,
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
            onImportArticlesClick = {},
            onExportArticlesClick = {},
        )
    }
}

@Preview
@Composable
private fun ImportExportLoadingPreview() {
    PreviewTheme {
        ImportExportContent(
            feedImportExportState = FeedImportExportState.LoadingExport(ImportExportContentType.ArticlesCsv),
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
            onImportArticlesClick = {},
            onExportArticlesClick = {},
        )
    }
}

@Preview
@Composable
private fun ImportExportArticleDonePreview() {
    PreviewTheme {
        ImportExportContent(
            feedImportExportState = FeedImportExportState.ArticleExportSuccess,
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
            onImportArticlesClick = {},
            onExportArticlesClick = {},
        )
    }
}
