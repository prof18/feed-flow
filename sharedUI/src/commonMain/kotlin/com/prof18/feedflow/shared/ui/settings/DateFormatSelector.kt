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
import androidx.compose.material.icons.outlined.DateRange
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.DateFormat
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun DateFormatSelector(
    currentFormat: DateFormat,
    onFormatSelected: (DateFormat) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            Icons.Outlined.DateRange,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.dateFormatTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentFormat) {
                    DateFormat.NORMAL -> LocalFeedFlowStrings.current.dateFormatNormal
                    DateFormat.AMERICAN -> LocalFeedFlowStrings.current.dateFormatAmerican
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        DateFormatSelectionDialog(
            currentFormat = currentFormat,
            onFormatSelected = onFormatSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun DateFormatSelectionDialog(
    currentFormat: DateFormat,
    onFormatSelected: (DateFormat) -> Unit,
    dismissDialog: () -> Unit,
) {
    Dialog(onDismissRequest = dismissDialog) {
        LazyColumn(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            items(DateFormat.entries) { format ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = format == currentFormat,
                            onClick = {
                                onFormatSelected(format)
                                dismissDialog()
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = format == currentFormat,
                        onClick = {
                            onFormatSelected(format)
                            dismissDialog()
                        },
                    )
                    Text(
                        text = when (format) {
                            DateFormat.NORMAL -> LocalFeedFlowStrings.current.dateFormatNormal
                            DateFormat.AMERICAN -> LocalFeedFlowStrings.current.dateFormatAmerican
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DateFormatSelectorPreview() {
    DateFormatSelector(
        currentFormat = DateFormat.NORMAL,
        onFormatSelected = {},
    )
}

@Preview
@Composable
private fun DateFormatSelectionDialogPreview() {
    DateFormatSelectionDialog(
        currentFormat = DateFormat.NORMAL,
        onFormatSelected = {},
        dismissDialog = {},
    )
}
