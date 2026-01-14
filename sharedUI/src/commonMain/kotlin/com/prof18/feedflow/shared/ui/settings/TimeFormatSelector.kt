package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.TimeFormat
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun TimeFormatSelector(
    currentFormat: TimeFormat,
    modifier: Modifier = Modifier,
    onFormatSelected: (TimeFormat) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.AccessTime,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.timeFormatTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentFormat) {
                    TimeFormat.HOURS_24 -> LocalFeedFlowStrings.current.timeFormatHours24
                    TimeFormat.HOURS_12 -> LocalFeedFlowStrings.current.timeFormatHours12
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        TimeFormatSelectionDialog(
            currentFormat = currentFormat,
            onFormatSelected = onFormatSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun TimeFormatSelectionDialog(
    currentFormat: TimeFormat,
    onFormatSelected: (TimeFormat) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = strings.timeFormatTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )
            LazyColumn {
                items(TimeFormat.entries) { format ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = format == currentFormat,
                                onClick = {
                                    onFormatSelected(format)
                                    dismissDialog()
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = format == currentFormat,
                            onClick = null,
                        )
                        Text(
                            text = when (format) {
                                TimeFormat.HOURS_24 -> strings.timeFormatHours24
                                TimeFormat.HOURS_12 -> strings.timeFormatHours12
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TimeFormatSelectorPreview() {
    TimeFormatSelector(
        currentFormat = TimeFormat.HOURS_24,
        onFormatSelected = {},
    )
}

@Preview
@Composable
private fun TimeFormatSelectionDialogPreview() {
    TimeFormatSelectionDialog(
        currentFormat = TimeFormat.HOURS_24,
        onFormatSelected = {},
        dismissDialog = {},
    )
}
