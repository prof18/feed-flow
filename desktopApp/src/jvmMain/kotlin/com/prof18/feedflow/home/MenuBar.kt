package com.prof18.feedflow.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
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
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {

            Item(
//                text = stringResource(MR.strings.my_string),
                text = "Refresh Feed",
                onClick = {
                    onRefreshClick()
                },
                shortcut = KeyShortcut(Key.R, meta = true)
            )

            Item(
                text = "Mark all read",
                onClick = {
                    onMarkAllReadClick()
                },
            )

            Item(
                text = "Clear old articles",
                onClick = {
                    onClearOldFeedClick()
                },
            )

            Separator()

            Item(
                text = "Feeds",
                onClick = {
                    onFeedsListClick()
                },
            )

            Item(
                text = "Import Feed from OPML",
                onClick = {
                    val fileChooser = JFileChooser("~").apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        addChoosableFileFilter(FileNameExtensionFilter("OPML", "opml"))
                        dialogTitle = "Select OPML file"
                        approveButtonText = "Import"
                    }
                    fileChooser.showOpenDialog(window)
                    val result = fileChooser.selectedFile
                    if (result != null) {
                        onImportOPMLCLick(result)
                    }
                },
            )

            Item(
                text = "Export Feeds to OPML",
                onClick = {
                    val fileChooser = JFileChooser().apply {
                        dialogTitle = "Save OPML File"
                        fileFilter = FileNameExtensionFilter("OPML Files (*.opml)", "opml")
                        approveButtonText = "Export"
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
    }
}