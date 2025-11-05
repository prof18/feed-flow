package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.AwtWindow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import com.prof18.feedflow.shared.ui.importexport.ImportExportContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import java.awt.FileDialog
import java.io.File
import javax.swing.JFrame

internal class ImportExportScreen(
    private val composeWindow: ComposeWindow,
    private val triggerFeedFetch: () -> Unit,
) : Screen {

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<ImportExportViewModel>() }
        val feedImporterState by viewModel.importExportState.collectAsState()
        val savedUrls by viewModel.savedUrls.collectAsState()

        val importDialogTitle = LocalFeedFlowStrings.current.importDialogTitle
        val exportDialogTitle = LocalFeedFlowStrings.current.exportDialogTitle

        var showImportDialog by remember { mutableStateOf(false) }
        var showExportDialog by remember { mutableStateOf(false) }

        val navigator = LocalNavigator.currentOrThrow

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
                },
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
                },
            )
        }

        ImportExportContent(
            navigateBack = {
                navigator.pop()
            },
            feedImportExportState = feedImporterState,
            savedUrls = savedUrls,
            onDoneClick = {
                triggerFeedFetch()
                navigator.pop()
            },
            onRetryClick = {
                viewModel.clearState()
            },
            onImportClick = {
                showImportDialog = true
            },
            onExportClick = {
                showExportDialog = true
            },
            onImportFromUrlClick = { url ->
                viewModel.importFeedFromUrl(url)
            },
            onReimportFromUrlClick = { url ->
                viewModel.importFeedFromUrl(url, saveUrl = false)
            },
            onDeleteUrlClick = { url ->
                viewModel.removeOpmlUrl(url)
            },
        )
    }
}

@Composable
private fun FileDialog(
    dialogTitle: String,
    parent: JFrame,
    onCloseRequest: (result: File?) -> Unit,
    exportFileName: String? = null,
    isLoadDialog: Boolean = false,
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
    dispose = FileDialog::dispose,
)
