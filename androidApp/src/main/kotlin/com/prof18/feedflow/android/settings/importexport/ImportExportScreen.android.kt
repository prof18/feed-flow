package com.prof18.feedflow.android.settings.importexport

import FeedFlowTheme
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

    ImportExportContent(
        navigateBack = navigateBack,
        feedImportExportState = feedImporterState,
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
            navigateBack = {},
            onRetryClick = {},
            onDoneClick = {},
            onImportClick = {},
            onExportClick = {},
        )
    }
}

private class ImportExportPreviewParameterProvider : PreviewParameterProvider<FeedImportExportState> {
    override val values: Sequence<FeedImportExportState> = importExportStates.asSequence()
}
