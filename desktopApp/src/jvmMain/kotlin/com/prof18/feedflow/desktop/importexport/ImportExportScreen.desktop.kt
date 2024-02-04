package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.awt.ComposeWindow
import com.prof18.feedflow.MR
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.ui.importexport.ImportExportContent
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

                val deviceName = getUnixDeviceName()
                val formattedDate = viewModel.getCurrentDateForExport()
                val fileName = "feedflow-export_${formattedDate}_$deviceName.opml".lowercase()

                selectedFile = File(fileName)
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
