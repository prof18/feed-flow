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
import androidx.compose.material.icons.outlined.FormatListNumbered
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
import com.prof18.feedflow.core.model.DescriptionLineLimit
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun DescriptionLineLimitSelector(
    currentLimit: DescriptionLineLimit,
    modifier: Modifier = Modifier,
    showLeadingIcon: Boolean = true,
    onLimitSelected: (DescriptionLineLimit) -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    var showDialog by remember { mutableStateOf(false) }

    val currentLabel = when (currentLimit) {
        DescriptionLineLimit.THREE -> strings.settingsDescriptionLinesThree
        DescriptionLineLimit.FIVE -> strings.settingsDescriptionLinesFive
        DescriptionLineLimit.NO_LIMIT -> strings.settingsDescriptionLinesNoLimit
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        if (showLeadingIcon) {
            Icon(
                Icons.Outlined.FormatListNumbered,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.regular),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = strings.settingsDescriptionMaxLines,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        DescriptionLineLimitSelectionDialog(
            currentLimit = currentLimit,
            onLimitSelected = onLimitSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun DescriptionLineLimitSelectionDialog(
    currentLimit: DescriptionLineLimit,
    onLimitSelected: (DescriptionLineLimit) -> Unit,
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
                text = strings.settingsDescriptionMaxLines,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )
            LazyColumn {
                items(DescriptionLineLimit.entries) { limit ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = limit == currentLimit,
                                onClick = {
                                    onLimitSelected(limit)
                                    dismissDialog()
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = limit == currentLimit,
                            onClick = null,
                        )
                        Text(
                            text = when (limit) {
                                DescriptionLineLimit.THREE -> strings.settingsDescriptionLinesThree
                                DescriptionLineLimit.FIVE -> strings.settingsDescriptionLinesFive
                                DescriptionLineLimit.NO_LIMIT -> strings.settingsDescriptionLinesNoLimit
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
private fun DescriptionLineLimitSelectorPreview() {
    DescriptionLineLimitSelector(
        currentLimit = DescriptionLineLimit.THREE,
        onLimitSelected = {},
    )
}
