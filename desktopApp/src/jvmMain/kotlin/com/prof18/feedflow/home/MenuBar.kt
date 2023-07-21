package com.prof18.feedflow.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import com.prof18.feedflow.MR
import dev.icerock.moko.resources.compose.stringResource
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    onRefreshClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onImportOPMLCLick: (File) -> Unit,
    onExportOPMLClick: (File) -> Unit,
    onFeedsListClick: () -> Unit,
    onClearOldFeedClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {
            Item(
                text = stringResource(resource = MR.strings.refresh_feeds),
                onClick = {
                    onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true),
            )

            Item(
                text = stringResource(resource = MR.strings.mark_all_read_button),
                onClick = {
                    onMarkAllReadClick()
                },
            )

            Item(
                text = stringResource(resource = MR.strings.clear_old_articles_button),
                onClick = {
                    onClearOldFeedClick()
                },
            )

            Separator()

            Item(
                text = stringResource(resource = MR.strings.feeds_title),
                onClick = {
                    onFeedsListClick()
                },
            )

            ImportFeedsButton(
                window = this@FeedFlowMenuBar.window,
                onImportOPMLCLick = onImportOPMLCLick,
            )

            ExportFeedsButton(
                onExportOPMLClick = onExportOPMLClick,
            )

            Separator()

            Item(
                text = stringResource(resource = MR.strings.about_button),
                onClick = onAboutClick,
            )
        }
    }
}

@Composable
private fun MenuScope.ImportFeedsButton(
    window: ComposeWindow,
    onImportOPMLCLick: (File) -> Unit,
) {
    val importDialogButton = stringResource(resource = MR.strings.import_dialog_button)
    val importDialogTitle = stringResource(resource = MR.strings.import_dialog_title)

    Item(
        text = stringResource(resource = MR.strings.import_feed_button),
        onClick = {
            val fileChooser = JFileChooser("~").apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                addChoosableFileFilter(FileNameExtensionFilter("OPML", "opml"))
                dialogTitle = importDialogTitle
                approveButtonText = importDialogButton
            }
            fileChooser.showOpenDialog(window)
            val result = fileChooser.selectedFile
            if (result != null) {
                onImportOPMLCLick(result)
            }
        },
    )
}

@Composable
private fun MenuScope.ExportFeedsButton(
    onExportOPMLClick: (File) -> Unit,
) {
    val exportDialogButton = stringResource(resource = MR.strings.export_dialog_button)
    val exportDialogTitle = stringResource(resource = MR.strings.export_dialog_title)

    Item(
        text = stringResource(resource = MR.strings.export_feeds_button),
        onClick = {
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
                onExportOPMLClick(outputFile)
            }
        },
    )
}
