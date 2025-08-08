package com.prof18.feedflow.android.settings.components

import FeedFlowTheme
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
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prof18.feedflow.shared.domain.model.Browser
import com.prof18.feedflow.shared.presentation.preview.browsersForPreview
import com.prof18.feedflow.shared.ui.preview.PreviewPhone
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun BrowserSelectionDialog(
    browserList: ImmutableList<Browser>,
    onBrowserSelected: (Browser) -> Unit,
    dismissDialog: () -> Unit,
) {
    Dialog(onDismissRequest = dismissDialog) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(Spacing.regular),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.browserSelectionButton,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular),
            )
            LazyColumn {
                items(browserList) { browser ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = browser.isFavourite,
                                onClick = {
                                    onBrowserSelected(browser)
                                    dismissDialog()
                                },
                                role = androidx.compose.ui.semantics.Role.RadioButton,
                            )
                            .padding(vertical = Spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = browser.isFavourite,
                            onClick = null,
                        )
                        Text(
                            text = browser.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Spacing.small),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrowserSelector(
    browsers: ImmutableList<Browser>,
    onBrowserSelected: (Browser) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentBrowser = browsers.firstOrNull { it.isFavourite }

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
            Icons.Outlined.Language,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = LocalFeedFlowStrings.current.browserSelectionButton,
                style = MaterialTheme.typography.bodyLarge,
            )
            currentBrowser?.let {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showDialog) {
        BrowserSelectionDialog(
            browserList = browsers,
            onBrowserSelected = { browser ->
                onBrowserSelected(browser)
                showDialog = false
            },
            dismissDialog = { showDialog = false },
        )
    }
}

@PreviewPhone
@Composable
private fun BrowserSelectionDialogPreview() {
    FeedFlowTheme {
        BrowserSelectionDialog(
            browserList = browsersForPreview,
            onBrowserSelected = {},
            dismissDialog = {},
        )
    }
}
