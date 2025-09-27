package com.prof18.feedflow.desktop.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.prof18.feedflow.desktop.about.AboutContent
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun AboutDialog(
    visible: Boolean,
    onCloseRequest: () -> Unit,
    version: String,
    isDarkTheme: Boolean,
) {
    val aboutDialogState = rememberDialogState(
        size = DpSize(500.dp, 500.dp),
    )

    DialogWindow(
        state = aboutDialogState,
        title = LocalFeedFlowStrings.current.appName,
        visible = visible,
        onCloseRequest = onCloseRequest,
    ) {
        AboutContent(
            versionLabel = LocalFeedFlowStrings.current.aboutAppVersion(version),
            isDarkTheme = isDarkTheme,
        )
    }
}
