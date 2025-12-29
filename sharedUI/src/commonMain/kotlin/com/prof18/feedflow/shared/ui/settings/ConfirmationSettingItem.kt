package com.prof18.feedflow.shared.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.prof18.feedflow.shared.ui.components.ConfirmationDialog
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ConfirmationSettingItem(
    title: String,
    icon: ImageVector,
    dialogTitle: String,
    dialogMessage: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButtonText: String = LocalFeedFlowStrings.current.confirmButton,
    dismissButtonText: String = LocalFeedFlowStrings.current.cancelButton,
    isDestructive: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingItem(
        title = title,
        icon = icon,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        ConfirmationDialog(
            title = dialogTitle,
            message = dialogMessage,
            onConfirm = onConfirm,
            onDismiss = { showDialog = false },
            confirmButtonText = confirmButtonText,
            dismissButtonText = dismissButtonText,
            isDestructive = isDestructive,
        )
    }
}

@Preview
@Composable
private fun ConfirmationSettingItemPreview() {
    PreviewTheme {
        ConfirmationSettingItem(
            title = "Clear Downloaded Articles",
            icon = Icons.Outlined.DeleteSweep,
            dialogTitle = "Clear Downloaded Articles",
            dialogMessage = "This will delete all downloaded article content. This action cannot be undone.",
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun ConfirmationSettingItemCachePreview() {
    PreviewTheme {
        ConfirmationSettingItem(
            title = "Clear Image Cache",
            icon = Icons.Outlined.Image,
            dialogTitle = "Clear Image Cache",
            dialogMessage = "This will clear all cached images. Images will be re-downloaded when needed.",
            onConfirm = {},
            isDestructive = true,
        )
    }
}
