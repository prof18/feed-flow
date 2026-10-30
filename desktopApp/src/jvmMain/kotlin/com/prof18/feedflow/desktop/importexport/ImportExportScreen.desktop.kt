package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.AwtWindow
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.FeedImportExportState
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
import com.prof18.feedflow.shared.domain.opml.OpmlInput
import com.prof18.feedflow.shared.domain.opml.OpmlOutput
import com.prof18.feedflow.shared.presentation.ImportExportViewModel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import java.awt.Dialog
import java.awt.FileDialog
import java.io.File

@Composable
internal fun ImportExportScreen(
    onCloseRequest: () -> Unit,
    triggerFeedFetch: () -> Unit,
    pickerState: ImportExportFilePickerState,
    viewModel: ImportExportViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.importExportState.collectAsState()
    ImportExportContent(
        modifier = modifier,
        isFilePickerOpen = pickerState.request != null,
        onCloseClick = onCloseRequest,
        feedImportExportState = state,
        onDoneClick = {
            if (state is FeedImportExportState.ImportSuccess || state is FeedImportExportState.ArticleImportSuccess) {
                triggerFeedFetch()
            }
            viewModel.clearState()
        },
        onRetryClick = viewModel::clearState,
        onChooseAnotherFileClick = {
            viewModel.clearState()
            pickerState.open(ImportExportFileAction.ImportFeeds)
        },
        onImportClick = { pickerState.open(ImportExportFileAction.ImportFeeds) },
        onExportClick = { pickerState.open(ImportExportFileAction.ExportFeeds) },
        onImportArticlesClick = { pickerState.open(ImportExportFileAction.ImportArticles) },
        onExportArticlesClick = { pickerState.open(ImportExportFileAction.ExportArticles, it) },
    )
}

@Composable
internal fun ImportExportFilePickers(
    owner: Dialog,
    pickerState: ImportExportFilePickerState,
    viewModel: ImportExportViewModel,
) {
    val request = pickerState.request ?: return
    val strings = LocalFeedFlowStrings.current
    val isLoad = request.action == ImportExportFileAction.ImportFeeds ||
        request.action == ImportExportFileAction.ImportArticles
    val title = when (request.action) {
        ImportExportFileAction.ImportFeeds -> strings.importDialogTitle
        ImportExportFileAction.ExportFeeds -> strings.exportDialogTitle
        ImportExportFileAction.ImportArticles -> strings.importArticlesDialogTitle
        ImportExportFileAction.ExportArticles -> strings.exportArticlesDialogTitle
    }
    val date = viewModel.getCurrentDateForExport()
    val device = getUnixDeviceName()
    val filename = when (request.action) {
        ImportExportFileAction.ExportFeeds -> "feedflow-export_${date}_$device.opml".lowercase()
        ImportExportFileAction.ExportArticles ->
            "feedflow-articles-export_${date}_${device}_${request.filter.name.lowercase()}.csv"
        else -> null
    }
    NativeImportExportFileDialog(
        parent = owner,
        dialogTitle = title,
        isLoadDialog = isLoad,
        exportFileName = filename,
        onCloseRequest = { file ->
            if (pickerState.request == request) {
                pickerState.close()
                viewModel.handleFileResult(request, file)
            }
        },
    )
}

private fun ImportExportViewModel.handleFileResult(request: ImportExportFileRequest, file: File?) {
    if (file == null) {
        if (request.action == ImportExportFileAction.ExportArticles) clearState()
        return
    }
    when (request.action) {
        ImportExportFileAction.ImportFeeds -> importFeed(OpmlInput(file))
        ImportExportFileAction.ExportFeeds -> exportFeed(OpmlOutput(file.withExtension("opml")))
        ImportExportFileAction.ImportArticles -> importArticles(CsvInput(file))
        ImportExportFileAction.ExportArticles -> exportArticles(CsvOutput(file.withExtension("csv")), request.filter)
    }
}

private fun File.withExtension(extension: String): File =
    if (name.endsWith(".$extension")) this else File("$absolutePath.$extension")

internal class ImportExportFilePickerState {
    var request by mutableStateOf<ImportExportFileRequest?>(null)
        private set

    fun open(action: ImportExportFileAction, filter: ArticleExportFilter = ArticleExportFilter.All) {
        if (request == null) request = ImportExportFileRequest(action, filter)
    }

    fun close() {
        request = null
    }
}

@Composable
internal fun rememberImportExportFilePickerState(): ImportExportFilePickerState =
    remember { ImportExportFilePickerState() }

internal enum class ImportExportFileAction { ImportFeeds, ExportFeeds, ImportArticles, ExportArticles }

internal data class ImportExportFileRequest(val action: ImportExportFileAction, val filter: ArticleExportFilter)

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
