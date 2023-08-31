package com.prof18.feedflow.settings.importexport

import FeedFlowTheme
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prof18.feedflow.MR
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.presentation.ImportExportViewModel
import com.prof18.feedflow.presentation.model.FeedImportExportState
import com.prof18.feedflow.presentation.preview.importExportStates
import com.prof18.feedflow.ui.importexport.ExportDoneView
import com.prof18.feedflow.ui.importexport.ImportDoneView
import com.prof18.feedflow.ui.importexport.ImportExportErrorView
import com.prof18.feedflow.ui.importexport.ImportExportIdleView
import com.prof18.feedflow.ui.importexport.ImportExportLoadingView
import com.prof18.feedflow.ui.importexport.ImportExportNavBar
import com.prof18.feedflow.ui.preview.FeedFlowPreview
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
            viewModel.clearErrorState()
        },
        onImportClick = {
            openFileAction.launch(arrayOf("*/*"))
        },
        onExportClick = {
            createFileAction.launch("feeds-export.opml")
        },
    )
}

@Composable
private fun ImportExportContent(
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

            FeedImportExportState.Loading ->
                ImportExportLoadingView(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
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

@FeedFlowPreview
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
