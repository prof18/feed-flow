package com.prof18.feedflow.desktop.startup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.database.DesktopDatabaseFailure
import com.prof18.feedflow.database.DesktopDatabaseFailureKind
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import kotlinx.coroutines.flow.Flow
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.OutlinedButton
import org.jetbrains.jewel.ui.component.Text
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Composable
internal fun DatabaseStartupErrorWindow(
    failure: DesktopDatabaseFailure,
    dataFolder: File,
    onQuit: () -> Unit,
    activationRequests: Flow<Unit>,
) {
    StartupMessageWindow(onCloseRequest = onQuit, activationRequests = activationRequests) {
        val strings = LocalFeedFlowStrings.current
        val message = when (failure.kind) {
            DesktopDatabaseFailureKind.Locked -> strings.databaseStartupLocked
            DesktopDatabaseFailureKind.StorageUnavailable -> strings.databaseStartupStorage
            DesktopDatabaseFailureKind.NativeLibrary -> strings.databaseStartupNative
            DesktopDatabaseFailureKind.Corrupt,
            DesktopDatabaseFailureKind.Unexpected,
            -> strings.databaseStartupGeneric
        }
        StartupMessage(
            message = message,
            detail = failure.sqliteResultCode,
            modifier = Modifier.weight(1f),
        )
        StartupActions {
            OutlinedButton(
                onClick = {
                    openDataFolder(
                        path = dataFolder,
                        open = { Desktop.getDesktop().open(it) },
                        copy = { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(it), null) },
                        canOpen = {
                            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
                        },
                    )
                },
            ) {
                Text(strings.databaseOpenDataFolder)
            }
            DefaultButton(onClick = onQuit) {
                Text(strings.databaseQuit)
            }
        }
    }
}

/**
 * Opens [path] in the file manager, falling back to copying it. Never throws: the error window must
 * stay usable even when neither works.
 */
internal fun openDataFolder(
    path: File,
    open: (File) -> Unit,
    copy: (String) -> Unit,
    canOpen: () -> Boolean = { true },
) {
    val opened = try {
        canOpen() && run {
            open(path)
            true
        }
    } catch (_: Exception) {
        false
    }
    if (opened) return
    try {
        // setContents throws IllegalStateException while another app holds the clipboard.
        copy(path.absolutePath)
    } catch (_: Exception) {
        // Nothing else to try; the window keeps working and Quit still exits.
    }
}
