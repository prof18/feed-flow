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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.core.model.FeedOrder
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
internal fun FeedOrderSelectionDialog(
    currentFeedOrder: FeedOrder,
    onFeedOrderSelected: (FeedOrder) -> Unit,
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
                text = strings.settingsFeedOrderTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )

            LazyColumn {
                item {
                    FeedOrderOption(
                        text = strings.settingsFeedOrderNewestFirst,
                        selected = currentFeedOrder == FeedOrder.NEWEST_FIRST,
                        onClick = {
                            onFeedOrderSelected(FeedOrder.NEWEST_FIRST)
                            dismissDialog()
                        },
                    )
                }
                item {
                    FeedOrderOption(
                        text = strings.settingsFeedOrderOldestFirst,
                        selected = currentFeedOrder == FeedOrder.OLDEST_FIRST,
                        onClick = {
                            onFeedOrderSelected(FeedOrder.OLDEST_FIRST)
                            dismissDialog()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedOrderOption(
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
                role = Role.RadioButton,
            )
            .padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = Spacing.small),
        )
    }
}

@PreviewPhone
@Composable
private fun FeedOrderSelectionDialogPreview() {
    FeedFlowTheme {
        FeedOrderSelectionDialog(
            currentFeedOrder = FeedOrder.NEWEST_FIRST,
            onFeedOrderSelected = {},
            dismissDialog = {},
        )
    }
}
