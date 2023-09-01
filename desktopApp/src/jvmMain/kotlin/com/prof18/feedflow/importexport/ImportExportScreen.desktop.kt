package com.prof18.feedflow.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.awt.ComposeWindow
import com.prof18.feedflow.MR
import com.prof18.feedflow.desktopViewModel
import com.prof18.feedflow.di.DI
import com.prof18.feedflow.domain.opml.OpmlInput
import com.prof18.feedflow.domain.opml.OpmlOutput
import com.prof18.feedflow.presentation.ImportExportViewModel
import com.prof18.feedflow.ui.importexport.ImportExportContent
import dev.icerock.moko.resources.compose.stringResource
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun ImportExportScreen(
    composeWindow: ComposeWindow,
    navigateBack: () -> Unit,
) {
    val viewModel = desktopViewModel { DI.koin.get<ImportExportViewModel>() }
    val feedImporterState by viewModel.importExportState.collectAsState()

    val importDialogButton = stringResource(resource = MR.strings.import_dialog_button)
    val importDialogTitle = stringResource(resource = MR.strings.import_dialog_title)

    val exportDialogButton = stringResource(resource = MR.strings.export_dialog_button)
    val exportDialogTitle = stringResource(resource = MR.strings.export_dialog_title)

    ImportExportContent(
        navigateBack = navigateBack,
        feedImportExportState = feedImporterState,
        onDoneClick = navigateBack,
        onRetryClick = {
            viewModel.clearState()
        },
        onImportClick = {
            val fileChooser = JFileChooser("~").apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                addChoosableFileFilter(FileNameExtensionFilter("OPML", "opml"))
                dialogTitle = importDialogTitle
                approveButtonText = importDialogButton
            }
            fileChooser.showOpenDialog(composeWindow)
            val result = fileChooser.selectedFile
            if (result != null) {
                viewModel.importFeed(OpmlInput(result))
            }
        },
        onExportClick = {
            val fileChooser = JFileChooser().apply {
                dialogTitle = exportDialogTitle
                fileFilter = FileNameExtensionFilter("OPML Files (*.opml)", "opml")
                approveButtonText = exportDialogButton
            }

            val userSelection = fileChooser.showSaveDialog(null)
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                var outputFile = fileChooser.selectedFile
                if (!outputFile.name.endsWith(".opml")) {
                    outputFile = File(outputFile.absolutePath + ".opml")
                }
                viewModel.exportFeed(OpmlOutput(outputFile))
            }
        },
    )
}
