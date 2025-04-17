package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
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
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SwipeActionSelector(
    direction: SwipeDirection,
    currentAction: SwipeActionType,
    onActionSelected: (SwipeActionType) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val strings = LocalFeedFlowStrings.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { showDialog = true }
            .fillMaxWidth()
            .padding(vertical = Spacing.xsmall)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            if (direction == SwipeDirection.LEFT) {
                Icons.Outlined.SwipeLeft
            } else {
                Icons.Outlined.SwipeRight
            },
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = if (direction == SwipeDirection.LEFT) {
                    strings.settingsLeftSwipeAction
                } else {
                    strings.settingsRightSwipeAction
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (currentAction) {
                    SwipeActionType.TOGGLE_READ_STATUS -> strings.settingsSwipeActionToggleRead
                    SwipeActionType.TOGGLE_BOOKMARK_STATUS -> strings.settingsSwipeActionToggleBookmark
                    SwipeActionType.NONE -> strings.settingsSwipeActionNone
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        SwipeActionDialog(
            direction = direction,
            currentAction = currentAction,
            onActionSelected = onActionSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun SwipeActionDialog(
    direction: SwipeDirection,
    currentAction: SwipeActionType,
    onActionSelected: (SwipeActionType) -> Unit,
    dismissDialog: () -> Unit,
) {
    val strings = LocalFeedFlowStrings.current
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = when (direction) {
                    SwipeDirection.LEFT -> strings.settingsLeftSwipeAction
                    SwipeDirection.RIGHT -> strings.settingsRightSwipeAction
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.regular),
            )

            LazyColumn {
                item {
                    ActionOption(
                        text = strings.settingsSwipeActionToggleRead,
                        selected = currentAction == SwipeActionType.TOGGLE_READ_STATUS,
                        onClick = {
                            onActionSelected(SwipeActionType.TOGGLE_READ_STATUS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    ActionOption(
                        text = strings.settingsSwipeActionToggleBookmark,
                        selected = currentAction == SwipeActionType.TOGGLE_BOOKMARK_STATUS,
                        onClick = {
                            onActionSelected(SwipeActionType.TOGGLE_BOOKMARK_STATUS)
                            dismissDialog()
                        },
                    )
                }
                item {
                    ActionOption(
                        text = strings.settingsSwipeActionNone,
                        selected = currentAction == SwipeActionType.NONE,
                        onClick = {
                            onActionSelected(SwipeActionType.NONE)
                            dismissDialog()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
private fun SwipeActionDialogPreview() {
    SwipeActionDialog(
        direction = SwipeDirection.LEFT,
        currentAction = SwipeActionType.TOGGLE_READ_STATUS,
        onActionSelected = {},
        dismissDialog = {},
    )
}
