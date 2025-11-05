package com.prof18.feedflow.shared.ui.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.shared.ui.settings.SettingItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ImportExportContent(
    feedImportExportState: FeedImportExportState,
    savedUrls: List<String>,
    navigateBack: () -> Unit,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportFromUrlClick: (String) -> Unit,
    onReimportFromUrlClick: (String) -> Unit,
    onDeleteUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ImportExportNavBar(navigateBack)
        },
    ) { paddingValues ->
        when (feedImportExportState) {
            is FeedImportExportState.Idle ->
                ImportExportIdleView(
                    modifier = Modifier.padding(paddingValues),
                    savedUrls = savedUrls,
                    onImportClick = onImportClick,
                    onExportClick = onExportClick,
                    onImportFromUrlClick = onImportFromUrlClick,
                    onReimportFromUrlClick = onReimportFromUrlClick,
                    onDeleteUrlClick = onDeleteUrlClick,
                )

            FeedImportExportState.Error ->
                ImportExportErrorView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    onRetryClick = onRetryClick,
                )

            FeedImportExportState.LoadingImport ->
                ImportExportLoadingView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    message = LocalFeedFlowStrings.current.feedAddInProgressMessage,
                )

            FeedImportExportState.LoadingExport ->
                ImportExportLoadingView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    message = LocalFeedFlowStrings.current.exportStartedMessage,
                )

            FeedImportExportState.ExportSuccess ->
                ExportDoneView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    onDoneClick = onDoneClick,
                )

            is FeedImportExportState.ImportSuccess ->
                ImportDoneView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    feedSourcesInvalid = feedImportExportState.notValidFeedSources,
                    feedSourceWithError = feedImportExportState.feedSourceWithError,
                    onDoneClick = onDoneClick,
                )
        }
    }
}

@Composable
private fun ImportExportNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(LocalFeedFlowStrings.current.importExportOpmlTitle)
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun ImportExportIdleView(
    savedUrls: List<String>,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportFromUrlClick: (String) -> Unit,
    onReimportFromUrlClick: (String) -> Unit,
    onDeleteUrlClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Text(
                modifier = Modifier
                    .padding(Spacing.regular),
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
                modifier = Modifier.padding(vertical = Spacing.regular),
                thickness = 0.2.dp,
                color = Color.Gray,
            )
        }

        item {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .padding(bottom = Spacing.small),
                text = LocalFeedFlowStrings.current.importFromUrlTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(LocalFeedFlowStrings.current.importFromUrlHint) },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = {
                        if (urlInput.isNotBlank()) {
                            onImportFromUrlClick(urlInput)
                            urlInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                    Text(LocalFeedFlowStrings.current.importFromUrlButton)
                }
            }
        }

        if (savedUrls.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.regular),
                    thickness = 0.2.dp,
                    color = Color.Gray,
                )
            }

            item {
                Text(
                    modifier = Modifier
                        .padding(horizontal = Spacing.regular)
                        .padding(bottom = Spacing.small),
                    text = LocalFeedFlowStrings.current.savedOpmlUrlsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            items(savedUrls) { url ->
                SavedUrlItem(
                    url = url,
                    onReimportClick = { onReimportFromUrlClick(url) },
                    onDeleteClick = { onDeleteUrlClick(url) },
                )
            }
        }
    }
}

@Composable
private fun SavedUrlItem(
    url: String,
    onReimportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Row {
            TextButton(onClick = onReimportClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = LocalFeedFlowStrings.current.reimportButton,
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = LocalFeedFlowStrings.current.deleteUrlButton,
                )
            }
        }
    }
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
