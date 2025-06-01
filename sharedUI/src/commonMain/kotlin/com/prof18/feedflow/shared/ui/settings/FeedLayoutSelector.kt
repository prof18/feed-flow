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
import androidx.compose.material.icons.outlined.RssFeed
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
import com.prof18.feedflow.core.model.FeedLayout
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun FeedLayoutSelector(
    feedLayout: FeedLayout,
    modifier: Modifier = Modifier,
    onFormatSelected: (FeedLayout) -> Unit,
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
            imageVector = Icons.Outlined.RssFeed,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.feedLayoutTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = when (feedLayout) {
                    FeedLayout.LIST -> LocalFeedFlowStrings.current.settingsFeedLayoutList
                    FeedLayout.CARD -> LocalFeedFlowStrings.current.settingsFeedLayoutCard
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDialog) {
        FeedLayoutSelectorDialog(
            currentFeedLayout = feedLayout,
            onFeedLayoutSelected = onFormatSelected,
            dismissDialog = { showDialog = false },
        )
    }
}

@Composable
private fun FeedLayoutSelectorDialog(
    currentFeedLayout: FeedLayout,
    onFeedLayoutSelected: (FeedLayout) -> Unit,
    dismissDialog: () -> Unit,
) {
    Dialog(onDismissRequest = dismissDialog) {
        LazyColumn(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            items(FeedLayout.entries) { feedLayout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = feedLayout == currentFeedLayout,
                            onClick = {
                                onFeedLayoutSelected(feedLayout)
                                dismissDialog()
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = feedLayout == currentFeedLayout,
                        onClick = {
                            onFeedLayoutSelected(feedLayout)
                            dismissDialog()
                        },
                    )
                    Text(
                        text = when (feedLayout) {
                            FeedLayout.LIST -> LocalFeedFlowStrings.current.settingsFeedLayoutList
                            FeedLayout.CARD -> LocalFeedFlowStrings.current.settingsFeedLayoutCard
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
private fun FeedLayoutSelectorPreview() {
    FeedLayoutSelector(
        feedLayout = FeedLayout.CARD,
        onFormatSelected = {},
    )
}

@Preview
@Composable
private fun FeedLayoutSelectorDialogPreview() {
    FeedLayoutSelectorDialog(
        currentFeedLayout = FeedLayout.CARD,
        onFeedLayoutSelected = {},
        dismissDialog = {},
    )
}
