package com.prof18.feedflow.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.prof18.feedflow.core.utils.getDesktopOS
import com.prof18.feedflow.core.utils.isMacOs

private val MacToolbarTitleAreaHeight = 30.dp

@Composable
internal fun DesktopDialogWindow(
    title: String,
    size: DpSize,
    visible: Boolean,
    onCloseRequest: () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val dialogState = rememberDialogState(size = size)
    val isMacOs = remember { getDesktopOS().isMacOs() }

    DialogWindow(
        state = dialogState,
        title = title,
        visible = visible,
        onCloseRequest = onCloseRequest,
    ) {
        LaunchedEffect(window) {
            if (isMacOs) {
                with(window.rootPane) {
                    putClientProperty("apple.awt.transparentTitleBar", true)
                    putClientProperty("apple.awt.fullWindowContent", true)
                    putClientProperty("apple.awt.windowTitleVisible", false)
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isMacOs) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MacToolbarTitleAreaHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                content(Modifier.fillMaxSize())
            }
        }
    }
}
