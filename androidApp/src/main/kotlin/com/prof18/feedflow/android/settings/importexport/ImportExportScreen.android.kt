package com.prof18.feedflow.android.settings.importexport

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.core.model.FeedImportExportState
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
    onDoneClick: () -> Unit,
) {
    val viewModel = koinViewModel<ImportExportViewModel>()

    val context = LocalContext.current
    val importingFeedMessage = LocalFeedFlowStrings.current.feedsImportingMessage

    val openFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            viewModel.importFeed(OpmlInput(context.contentResolver.openInputStream(uri)))
            Toast.makeText(context, importingFeedMessage, Toast.LENGTH_SHORT)
                .show()
        }
    }

    val createFileAction = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/x-opml"),
    ) { uri ->
        uri?.let {
            viewModel.exportFeed(OpmlOutput(context.contentResolver.openOutputStream(it)))
        }
    }

    val feedImporterState by viewModel.importExportState.collectAsStateWithLifecycle()
    val savedUrls by viewModel.savedUrls.collectAsStateWithLifecycle()

    ImportExportContent(
        navigateBack = navigateBack,
        feedImportExportState = feedImporterState,
        savedUrls = savedUrls,
        onDoneClick = onDoneClick,
        onRetryClick = {
            viewModel.clearState()
        },
        onImportClick = {
            openFileAction.launch(arrayOf("*/*"))
        },
        onExportClick = {
            val deviceName = "${Build.MANUFACTURER}-${Build.MODEL}"
            val formattedDate = viewModel.getCurrentDateForExport()
            val fileName = "feedflow-export_${formattedDate}_$deviceName.opml".lowercase()
            createFileAction.launch(fileName)
        },
        onImportFromUrlClick = { url ->
            viewModel.importFeedFromUrl(url)
            Toast.makeText(context, importingFeedMessage, Toast.LENGTH_SHORT).show()
        },
        onReimportFromUrlClick = { url ->
            viewModel.importFeedFromUrl(url, saveUrl = false)
            Toast.makeText(context, importingFeedMessage, Toast.LENGTH_SHORT).show()
        },
        onDeleteUrlClick = { url ->
            viewModel.removeOpmlUrl(url)
        },
    )
}

@PreviewPhone
@Composable
private fun ImportExportContentPreview(
    @PreviewParameter(ImportExportPreviewParameterProvider::class) state: FeedImportExportState,
) {
    FeedFlowTheme {
        ImportExportContent(
            feedImportExportState = state,
            savedUrls = listOf("https://example.com/feeds.opml"),
            navigateBack = {},
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
            onImportFromUrlClick = {},
            onReimportFromUrlClick = {},
            onDeleteUrlClick = {},
        )
    }
}

private class ImportExportPreviewParameterProvider : PreviewParameterProvider<FeedImportExportState> {
    override val values: Sequence<FeedImportExportState> = importExportStates.asSequence()
}
