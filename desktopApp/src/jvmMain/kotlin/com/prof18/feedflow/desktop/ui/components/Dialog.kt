package com.prof18.feedflow.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowScope

@Composable
internal fun Dialog(
    title: String,
    onCloseRequest: () -> Unit,
    size: DpSize = DpSize(400.dp, 300.dp),
    extendBehindTitleBar: Boolean = false,
    alertStyle: Boolean = false,
    content: @Composable DialogWindowScope.() -> Unit,
) {
    DesktopDialogWindow(
        title = title,
        size = size,
        onCloseRequest = onCloseRequest,
        extendBehindTitleBar = extendBehindTitleBar,
        alertStyle = alertStyle,
    ) { content() }
}
