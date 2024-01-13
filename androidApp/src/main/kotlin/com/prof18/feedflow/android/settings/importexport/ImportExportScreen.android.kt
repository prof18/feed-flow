package com.prof18.feedflow.android.settings.importexport

import FeedFlowTheme
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.presentation.preview.importExportStates
import com.prof18.feedflow.shared.ui.importexport.ImportExportContent
import com.prof18.feedflow.shared.ui.preview.FeedFlowPhonePreview
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ImportExportScreen(
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<ImportExportViewModel>()

    val context = LocalContext.current
    val importingFeedMessage = stringResource(resource = MR.strings.feeds_importing_message)

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
        onDoneClick = navigateBack,
        onRetryClick = {
            viewModel.clearState()
        },
        onImportClick = {
            openFileAction.launch(arrayOf("*/*"))
        },
        onExportClick = {
            createFileAction.launch("feeds-export.opml")
        },
    )
}

@FeedFlowPhonePreview
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
