package com.prof18.feedflow.desktop.importexport

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.ui.awt.ComposeWindow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prof18.feedflow.core.model.ArticleExportFilter
import com.prof18.feedflow.core.model.ImportExportContentType
import com.prof18.feedflow.desktop.desktopViewModel
import com.prof18.feedflow.desktop.di.DI
import com.prof18.feedflow.desktop.utils.generateUniqueKey
import com.prof18.feedflow.desktop.utils.getUnixDeviceName
import com.prof18.feedflow.shared.domain.csv.CsvInput
import com.prof18.feedflow.shared.domain.csv.CsvOutput
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

    override val key: String = generateUniqueKey()

    @Composable
    override fun Content() {
        val viewModel = desktopViewModel { DI.koin.get<ImportExportViewModel>() }
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

        if (showImportArticlesDialog) {
            FileDialog(
                parent = composeWindow,
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

            FileDialog(
                parent = composeWindow,
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
            navigateBack = {
                navigator.pop()
            },
            feedImportExportState = feedImporterState,
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
            onImportArticlesClick = {
                showImportArticlesDialog = true
            },
            onExportArticlesClick = { filter ->
                articleExportFilter = filter
                viewModel.startExport(ImportExportContentType.ArticlesCsv)
                showExportArticlesDialog = true
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
