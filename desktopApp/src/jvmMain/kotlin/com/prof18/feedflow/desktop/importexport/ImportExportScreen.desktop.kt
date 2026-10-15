package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.ui.components.Dialog
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Dialog
import java.awt.FileDialog
import java.io.File

@Composable
internal fun ImportExportScreen(
    onCloseRequest: () -> Unit,
    triggerFeedFetch: () -> Unit,
) {
    val viewModel = koinViewModel<ImportExportViewModel>()
    val feedImporterState by viewModel.importExportState.collectAsState()

    val importDialogTitle = LocalFeedFlowStrings.current.importDialogTitle
    val exportDialogTitle = LocalFeedFlowStrings.current.exportDialogTitle
    val importArticlesDialogTitle = LocalFeedFlowStrings.current.importArticlesDialogTitle
    val exportArticlesDialogTitle = LocalFeedFlowStrings.current.exportArticlesDialogTitle

    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportArticlesDialog by remember { mutableStateOf(false) }
    var showExportArticlesDialog by remember { mutableStateOf(false) }
    var articleExportFilter by remember { mutableStateOf(ArticleExportFilter.All) }
    val closeDialog = {
        showImportDialog = false
        showExportDialog = false
        showImportArticlesDialog = false
        showExportArticlesDialog = false
        viewModel.clearState()
        onCloseRequest()
    }

    ImportExportWindow(onCloseRequest = closeDialog) { owner ->
        if (showImportDialog) {
            NativeImportExportFileDialog(
                parent = owner,
                dialogTitle = importDialogTitle,
                isLoadDialog = true,
                allowedExtensions = persistentSetOf(),
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

            NativeImportExportFileDialog(
                parent = owner,
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

        if (showImportArticlesDialog) {
            NativeImportExportFileDialog(
                parent = owner,
                dialogTitle = importArticlesDialogTitle,
                isLoadDialog = true,
                onCloseRequest = { result ->
                    showImportArticlesDialog = false
                    if (result != null) {
                        viewModel.importArticles(CsvInput(result))
                    }
                },
            )
        }

        if (showExportArticlesDialog) {
            val deviceName = getUnixDeviceName()
            val formattedDate = viewModel.getCurrentDateForExport()
            val fileName =
                """
                    feedflow-articles-export_${formattedDate}_${deviceName}_${articleExportFilter.name.lowercase()}.csv
                """.trimIndent()

            NativeImportExportFileDialog(
                parent = owner,
                dialogTitle = exportArticlesDialogTitle,
                exportFileName = fileName,
                onCloseRequest = { result ->
                    showExportArticlesDialog = false
                    if (result != null) {
                        var outputFile = result

                        if (!outputFile.name.endsWith(".csv")) {
                            outputFile = File(outputFile.absolutePath + ".csv")
                        }
                        viewModel.exportArticles(
                            csvOutput = CsvOutput(outputFile),
                            filter = articleExportFilter,
                        )
                    } else {
                        viewModel.clearState()
                    }
                },
            )
        }

        ImportExportContent(
            isFilePickerOpen = showImportDialog || showExportDialog ||
                showImportArticlesDialog || showExportArticlesDialog,
            onCloseClick = closeDialog,
            feedImportExportState = feedImporterState,
            onDoneClick = {
                val isImportResult = feedImporterState is FeedImportExportState.ImportSuccess ||
                    feedImporterState is FeedImportExportState.ArticleImportSuccess
                if (isImportResult) {
                    triggerFeedFetch()
                }
                viewModel.clearState()
            },
            onRetryClick = {
                viewModel.clearState()
            },
            onChooseAnotherFileClick = {
                viewModel.clearState()
                showImportDialog = true
            },
            onImportClick = {
                showImportDialog = true
            },
            onExportClick = {
                showExportDialog = true
            },
            onImportArticlesClick = {
                showImportArticlesDialog = true
            },
            onExportArticlesClick = { filter ->
                articleExportFilter = filter
                showExportArticlesDialog = true
            },
        )
    }
}

@Composable
internal fun ImportExportWindow(
    onCloseRequest: () -> Unit,
    content: @Composable (Dialog) -> Unit,
) {
    Dialog(
        title = LocalFeedFlowStrings.current.importExportOpmlTitle,
        size = DpSize(500.dp, 600.dp),
        onCloseRequest = onCloseRequest,
    ) { content(window) }
}

@Composable
internal fun NativeImportExportFileDialog(
    dialogTitle: String,
    parent: Dialog,
    onCloseRequest: (result: File?) -> Unit,
    exportFileName: String? = null,
    isLoadDialog: Boolean = false,
    allowedExtensions: ImmutableSet<String> = persistentSetOf(),
) = AwtWindow(
    create = {
        createImportExportFileDialog(
            parent = parent,
            dialogTitle = dialogTitle,
            onCloseRequest = onCloseRequest,
            exportFileName = exportFileName,
            isLoadDialog = isLoadDialog,
            allowedExtensions = allowedExtensions,
        )
    },
    dispose = FileDialog::dispose,
)

internal fun createImportExportFileDialog(
    parent: Dialog,
    dialogTitle: String,
    onCloseRequest: (File?) -> Unit,
    exportFileName: String? = null,
    isLoadDialog: Boolean = false,
    allowedExtensions: ImmutableSet<String> = persistentSetOf(),
): FileDialog {
    val flag = if (isLoadDialog) FileDialog.LOAD else FileDialog.SAVE
    val dialog = object : FileDialog(parent, dialogTitle, flag) {
        override fun setVisible(value: Boolean) {
            super.setVisible(value)
            if (value) {
                onCloseRequest(files.firstOrNull())
            }
        }
    }
    if (isLoadDialog && allowedExtensions.isNotEmpty()) {
        dialog.filenameFilter = java.io.FilenameFilter { _, name ->
            val lowercaseName = name.lowercase()
            allowedExtensions.any { extension -> lowercaseName.endsWith(".$extension") }
        }
    }
    dialog.file = exportFileName
    return dialog
}
