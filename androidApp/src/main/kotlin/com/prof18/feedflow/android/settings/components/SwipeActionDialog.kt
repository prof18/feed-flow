package com.prof18.feedflow.android.settings.components

import FeedFlowTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.SwipeActionType
import com.prof18.feedflow.core.model.SwipeDirection
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun SwipeActionDialog(
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

@PreviewPhone
@Composable
private fun SwipeActionDialogPreview() {
    FeedFlowTheme {
        SwipeActionDialog(
            direction = SwipeDirection.LEFT,
            currentAction = SwipeActionType.TOGGLE_READ_STATUS,
            onActionSelected = {},
            dismissDialog = {},
        )
    }
}
