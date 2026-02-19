package com.prof18.feedflow.android.settings.feedsandaccounts.subpages

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.preview.importExportStates
import com.prof18.feedflow.shared.ui.importexport.ImportExportContent
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportExportScreen(
    navigateBack: () -> Unit,
    refreshFeeds: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<ImportExportViewModel>()

    val context = LocalContext.current
    val importingFeedMessage = LocalFeedFlowStrings.current.feedsImportingMessage
    val importingArticlesMessage = LocalFeedFlowStrings.current.articlesImportingMessage
    var articleExportFilter by remember { mutableStateOf(ArticleExportFilter.All) }

    val openOpmlFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.importFeed(OpmlInput(context.contentResolver.openInputStream(uri)))
            Toast.makeText(context, importingFeedMessage, Toast.LENGTH_SHORT)
                .show()
        }
    }

    val openArticlesFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.importArticles(CsvInput(context.contentResolver.openInputStream(uri)))
            Toast.makeText(context, importingArticlesMessage, Toast.LENGTH_SHORT)
                .show()
        }
    }

    val createOpmlFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) { uri ->
        uri?.let {
            viewModel.exportFeed(OpmlOutput(context.contentResolver.openOutputStream(it)))
        }
    }

    val createArticlesFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        uri?.let {
            viewModel.exportArticles(
                csvOutput = CsvOutput(context.contentResolver.openOutputStream(it)),
                filter = articleExportFilter,
            )
        } ?: viewModel.clearState()
    }

    val feedImporterState by viewModel.importExportState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = LocalFeedFlowStrings.current.importExportOpmlTitle)
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val layoutDir = LocalLayoutDirection.current
        ImportExportContent(
            feedImportExportState = feedImporterState,
            onDoneClick = {
                refreshFeeds()
                viewModel.clearState()
            },
            onRetryClick = {
                viewModel.clearState()
            },
            onImportClick = {
                openOpmlFileAction.launch(arrayOf("*/*"))
            },
            onExportClick = {
                val deviceName = "${Build.MANUFACTURER}-${Build.MODEL}"
                val formattedDate = viewModel.getCurrentDateForExport()
                val fileName = "feedflow-export_${formattedDate}_$deviceName.opml".lowercase()
                createOpmlFileAction.launch(fileName)
            },
            onImportArticlesClick = {
                openArticlesFileAction.launch(arrayOf("*/*"))
            },
            onExportArticlesClick = { filter ->
                articleExportFilter = filter
                viewModel.startExport(ImportExportContentType.ArticlesCsv)
                val deviceName = "${Build.MANUFACTURER}-${Build.MODEL}"
                val formattedDate = viewModel.getCurrentDateForExport()
                val fileName = "feedflow-articles-export_${formattedDate}_${deviceName}_${filter.name.lowercase()}.csv"
                createArticlesFileAction.launch(fileName)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(start = paddingValues.calculateLeftPadding(layoutDir))
                .padding(end = paddingValues.calculateRightPadding(layoutDir))
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}

@PreviewPhone
@Composable
private fun ImportExportContentPreview(
    @PreviewParameter(ImportExportPreviewParameterProvider::class) state: FeedImportExportState,
) {
    FeedFlowTheme {
        ImportExportContent(
            feedImportExportState = state,
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
            onImportArticlesClick = {},
            onExportArticlesClick = {},
        )
    }
}

private class ImportExportPreviewParameterProvider : PreviewParameterProvider<FeedImportExportState> {
    override val values: Sequence<FeedImportExportState> = importExportStates.asSequence()
}
