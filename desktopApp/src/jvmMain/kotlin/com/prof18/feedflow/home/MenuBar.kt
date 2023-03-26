package com.prof18.feedflow.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FrameWindowScope.FeedFlowMenuBar(
    onRefreshClick: () -> Unit,
    onMarkAllReadClick: () -> Unit,
    onImportOPMLCLick: (File) -> Unit,
    onFeedsListClick: () -> Unit,
    onAddFeedClick: () -> Unit,
) {
    MenuBar {
        Menu("File", mnemonic = 'F') {

            Item(
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

            Separator()

            Item(
                text = "Import Feed from OPML",
                onClick = {
                    val fileChooser = JFileChooser("/").apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        addChoosableFileFilter(FileNameExtensionFilter("OPML", "opml"))
                        dialogTitle = "Select OPML file"
                        approveButtonText = "Import"
                    }
                    fileChooser.showOpenDialog(window /* OR null */)
                    val result = fileChooser.selectedFile
                    onImportOPMLCLick(result)
                },
            )

            Item(
                text = "Add Feed",
                onClick = {
                    onAddFeedClick()
                },
            )

            Item(
                text = "Feeds",
                onClick = {
                    onFeedsListClick()
                },
            )
        }
    }
}