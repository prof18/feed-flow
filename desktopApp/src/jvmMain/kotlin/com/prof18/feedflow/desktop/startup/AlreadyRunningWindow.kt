package com.prof18.feedflow.desktop.startup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.desktop.ui.LocalFeedFlowStrings
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun AlreadyRunningWindow(
    onQuit: () -> Unit,
) {
    StartupMessageWindow(onCloseRequest = onQuit) {
        val strings = LocalFeedFlowStrings.current
        StartupMessage(
            message = strings.desktopAlreadyRunning,
            detail = null,
            modifier = Modifier.weight(1f),
        )
        StartupActions {
            DefaultButton(onClick = onQuit) {
                Text(strings.databaseQuit)
            }
        }
    }
}
