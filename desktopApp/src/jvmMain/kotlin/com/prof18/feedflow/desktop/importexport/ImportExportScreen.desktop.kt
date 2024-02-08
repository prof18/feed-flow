package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.AwtWindow
import com.prof18.feedflow.MR
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.ui.importexport.ImportExportContent
import dev.icerock.moko.resources.compose.stringResource
import java.awt.FileDialog
import java.io.File
import javax.swing.JFrame

@Composable
fun ImportExportScreen(
    composeWindow: ComposeWindow,
    navigateBack: () -> Unit,
) {
    val viewModel = desktopViewModel { DI.koin.get<ImportExportViewModel>() }
    val feedImporterState by viewModel.importExportState.collectAsState()

    val importDialogTitle = stringResource(resource = MR.strings.import_dialog_title)
    val exportDialogTitle = stringResource(resource = MR.strings.export_dialog_title)

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        FileDialog(
            parent = composeWindow,
            dialogTitle = importDialogTitle,
            isLoadDialog = true,
            onCloseRequest = { result ->
                showImportDialog = false
                if (result != null) {
                    viewModel.importFeed(OpmlInput(result))
                }
            }
        )
    }

    if (showExportDialog) {
        val deviceName = getUnixDeviceName()
        val formattedDate = viewModel.getCurrentDateForExport()
        val fileName = "feedflow-export_${formattedDate}_$deviceName.opml".lowercase()

        FileDialog(
            parent = composeWindow,
            dialogTitle = exportDialogTitle,
            exportFileName = fileName,
            onCloseRequest = { result ->
                showExportDialog = false
                if (result != null) {
                    var outputFile = result

                    if (!outputFile.name.endsWith(".opml")) {
                        outputFile = File(outputFile.absolutePath + ".opml")
                    }
                    viewModel.exportFeed(OpmlOutput(outputFile))
                }
            }
        )
    }

    ImportExportContent(
        navigateBack = navigateBack,
        feedImportExportState = feedImporterState,
        onDoneClick = navigateBack,
        onRetryClick = {
            viewModel.clearState()
        },
        onImportClick = {
            showImportDialog = true
        },
        onExportClick = {
            showExportDialog = true
        },
    )
}

@Composable
private fun FileDialog(
    parent: JFrame,
    isLoadDialog: Boolean = false,
    dialogTitle: String,
    exportFileName: String? = null,
    onCloseRequest: (result: File?) -> Unit
) = AwtWindow(
    create = {
        val flag = if (isLoadDialog) FileDialog.LOAD else FileDialog.SAVE
        val dialog = object : FileDialog(parent, dialogTitle, flag) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(files.firstOrNull())
                }
            }
        }
        dialog.file = exportFileName
        dialog
    },
    dispose = FileDialog::dispose
)
