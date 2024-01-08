package com.prof18.feedflow.ui.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ParsedFeedSource
import com.prof18.feedflow.ui.settings.SettingItem
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ImportExportContent(
    feedImportExportState: FeedImportExportState,
    navigateBack: () -> Unit,
    onRetryClick: () -> Unit,
    onDoneClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            ImportExportNavBar(navigateBack)
        },
    ) { paddingValues ->
        when (feedImportExportState) {
            is FeedImportExportState.Idle ->
                ImportExportIdleView(
                    modifier = Modifier.padding(paddingValues),
                    onImportClick = onImportClick,
                    onExportClick = onExportClick,
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
                    message = stringResource(MR.strings.feed_add_in_progress_message),
                )

            FeedImportExportState.LoadingExport ->
                ImportExportLoadingView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    stringResource(MR.strings.export_started_message),
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
                    feedSources = feedImportExportState.notValidFeedSources,
                    onDoneClick = onDoneClick,
                )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ImportExportNavBar(navigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(resource = MR.strings.import_export_opml_title),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navigateBack()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun ImportExportIdleView(
    modifier: Modifier = Modifier,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
    ) {
        item {
            Text(
                modifier = Modifier
                    .padding(Spacing.regular),
                color = MaterialTheme.colorScheme.onBackground,
                text = stringResource(MR.strings.import_export_description),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        item {
            SettingItem(
                title = stringResource(resource = MR.strings.import_feed_button),
                icon = Icons.Default.FileDownload,
                onClick = onImportClick,
            )
        }

        item {
            SettingItem(
                title = stringResource(resource = MR.strings.export_feeds_button),
                icon = Icons.Default.FileUpload,
                onClick = onExportClick,
            )
        }
    }
}

@Composable
private fun ImportExportLoadingView(
    modifier: Modifier = Modifier,
    message: String,
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
            text = stringResource(resource = MR.strings.generic_error_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onRetryClick,
        ) {
            Text(stringResource(resource = MR.strings.retry_button))
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
            text = stringResource(MR.strings.feeds_export_done_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            modifier = Modifier
                .padding(top = Spacing.regular),
            onClick = onDoneClick,
        ) {
            Text(stringResource(resource = MR.strings.done_button))
        }
    }
}

@Composable
private fun ImportDoneView(
    modifier: Modifier = Modifier,
    feedSources: List<ParsedFeedSource>,
    onDoneClick: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        if (feedSources.isEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(MR.strings.feeds_import_done_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    modifier = Modifier
                        .padding(top = Spacing.regular),
                    onClick = onDoneClick,
                ) {
                    Text(stringResource(resource = MR.strings.done_button))
                }
            }
        } else {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular)
                    .padding(top = Spacing.regular),
                text = stringResource(MR.strings.wrong_link_report_title),
                style = MaterialTheme.typography.titleMedium,
            )

            FeedsNotAddedList(
                modifier = Modifier.Companion
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
                Text(stringResource(resource = MR.strings.done_button))
            }
        }
    }
}

@Composable
private fun FeedsNotAddedList(
    modifier: Modifier = Modifier,
    feedSources: List<ParsedFeedSource>,
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

                Divider(
                    modifier = Modifier,
                    thickness = 0.2.dp,
                    color = Color.Gray,
                )
            }
        }
    }
}
