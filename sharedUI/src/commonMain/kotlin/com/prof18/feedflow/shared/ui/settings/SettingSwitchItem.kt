package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.prof18.feedflow.shared.ui.components.ConfirmationDialog
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SettingSwitchItem(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    confirmationDialog: ConfirmationDialogConfig? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var showConfirmation by remember { mutableStateOf(false) }
    var pendingValue by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable {
                if (!isChecked && confirmationDialog != null) {
                    pendingValue = true
                    showConfirmation = true
                } else {
                    onCheckedChange(!isChecked)
                }
            }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            icon,
            contentDescription = null,
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        Switch(
            interactionSource = interactionSource,
            checked = isChecked,
            onCheckedChange = { checked ->
                if (checked && confirmationDialog != null) {
                    pendingValue = checked
                    showConfirmation = true
                } else {
                    onCheckedChange(checked)
                }
            },
        )
    }

    if (showConfirmation && confirmationDialog != null) {
        ConfirmationDialog(
            title = confirmationDialog.title,
            message = confirmationDialog.message,
            confirmButtonText = confirmationDialog.confirmButtonText
                ?: LocalFeedFlowStrings.current.confirmButton,
            dismissButtonText = confirmationDialog.dismissButtonText
                ?: LocalFeedFlowStrings.current.cancelButton,
            onConfirm = {
                onCheckedChange(pendingValue)
            },
            onDismiss = {
                showConfirmation = false
            },
        )
    }
}

data class ConfirmationDialogConfig(
    val title: String,
    val message: String,
    val confirmButtonText: String? = null,
    val dismissButtonText: String? = null,
)

@Preview
@Composable
private fun SettingSwitchItemPreview() {
    PreviewTheme {
        SettingSwitchItem(
            title = "Dark Mode",
            icon = Icons.Outlined.DarkMode,
            isChecked = true,
            onCheckedChange = {},
        )
    }
}

@Preview
@Composable
private fun SettingSwitchItemWithConfirmationPreview() {
    PreviewTheme {
        SettingSwitchItem(
            title = "Prefetch Article Content",
            icon = Icons.Outlined.CloudDownload,
            isChecked = false,
            onCheckedChange = {},
            confirmationDialog = ConfirmationDialogConfig(
                title = "Prefetch Article Content",
                message = "This will download article content in the background and may use significant data.",
            ),
        )
    }
}
