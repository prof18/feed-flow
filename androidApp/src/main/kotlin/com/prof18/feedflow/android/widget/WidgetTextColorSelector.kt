package com.prof18.feedflow.android.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TextFields
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
import com.prof18.feedflow.shared.domain.model.WidgetTextColorMode
import com.prof18.feedflow.shared.ui.settings.SettingSelectorItem
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.theme.FeedFlowTheme
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun WidgetTextColorSelector(
    currentMode: WidgetTextColorMode,
    onModeSelected: (WidgetTextColorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingSelectorItem(
        title = LocalFeedFlowStrings.current.widgetTextColorTitle,
        currentValueLabel = currentMode.toLabel(),
        icon = Icons.Outlined.TextFields,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        WidgetTextColorSelectionDialog(
            currentMode = currentMode,
            onModeSelected = onModeSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun WidgetTextColorSelectionDialog(
    currentMode: WidgetTextColorMode,
    onModeSelected: (WidgetTextColorMode) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current

    Dialog(onDismissRequest = dismissDialog) {
        LazyColumn(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            item {
                Text(
                    text = strings.widgetTextColorTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .padding(bottom = Spacing.regular),
                )
            }

            items(WidgetTextColorMode.entries) { mode ->
                Row(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .selectable(
                            selected = mode == currentMode,
                            onClick = {
                                onModeSelected(mode)
                                dismissDialog()
                            },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = mode == currentMode,
                        onClick = null,
                    )
                    Text(
                        text = mode.toLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = Spacing.small),
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetTextColorMode.toLabel(): String {
    val strings = LocalFeedFlowStrings.current
    return when (this) {
        WidgetTextColorMode.AUTOMATIC -> strings.widgetTextColorAutomatic
        WidgetTextColorMode.LIGHT -> strings.widgetTextColorLight
        WidgetTextColorMode.DARK -> strings.widgetTextColorDark
    }
}

@Preview
@Composable
private fun WidgetTextColorSelectorPreview() {
    FeedFlowTheme {
        WidgetTextColorSelector(
            currentMode = WidgetTextColorMode.AUTOMATIC,
            onModeSelected = {},
        )
    }
}
