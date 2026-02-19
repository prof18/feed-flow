package com.prof18.feedflow.shared.ui.feed.editfeed

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import com.prof18.feedflow.core.model.CategoriesState
import com.prof18.feedflow.core.model.FeedSourceSettings
import com.prof18.feedflow.core.model.LinkOpeningPreference
import com.prof18.feedflow.shared.ui.feed.CategoriesSelector
import com.prof18.feedflow.shared.ui.feed.FeedNameTextField
import com.prof18.feedflow.shared.ui.feed.FeedUrlTextField
import com.prof18.feedflow.shared.ui.feed.LinkOpeningPreferenceSelector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun EditFeedContent(
    feedUrl: String,
    feedName: String,
    showError: Boolean,
    showLoading: Boolean,
    errorMessage: String,
    canEditUrl: Boolean,
    categoriesState: CategoriesState,
    feedSourceSettings: FeedSourceSettings,
    onFeedUrlUpdated: (String) -> Unit,
    onFeedNameUpdated: (String) -> Unit,
    onLinkOpeningPreferenceSelected: (LinkOpeningPreference) -> Unit,
    onHiddenToggled: (Boolean) -> Unit,
    onPinnedToggled: (Boolean) -> Unit,
    editFeed: () -> Unit,
    onCategorySelectorClick: () -> Unit,
    showDeleteDialog: Boolean,
    onShowDeleteDialog: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onNotificationToggleChanged: (Boolean) -> Unit = {},
    showNotificationToggle: Boolean = false,
) {
    if (showDeleteDialog) {
        var deleteInProgressState by remember(showDeleteDialog) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                if (!deleteInProgressState) onDismissDeleteDialog()
            },
            title = { Text(LocalFeedFlowStrings.current.deleteFeedConfirmationTitle) },
            text = { Text(LocalFeedFlowStrings.current.deleteFeedConfirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteInProgressState = true
                        onConfirmDelete()
                    },
                    enabled = !deleteInProgressState,
                ) {
                    if (deleteInProgressState) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    } else {
                        Text(LocalFeedFlowStrings.current.deleteFeed)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteDialog,
                    enabled = !deleteInProgressState,
                ) {
                    Text(LocalFeedFlowStrings.current.deleteCategoryCloseButton)
                }
            },
        )
    }
    val layoutDir = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier
            .padding(top = contentPadding.calculateTopPadding())
            .padding(start = contentPadding.calculateLeftPadding(layoutDir))
            .padding(end = contentPadding.calculateRightPadding(layoutDir))
            .padding(horizontal = Spacing.regular),
    ) {
        item {
            FeedNameTextField(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .fillMaxWidth(),
                feedName = feedName,
                onFeedNameUpdated = onFeedNameUpdated,
            )
        }

        if (canEditUrl) {
            item {
                FeedUrlTextField(
                    modifier = Modifier
                        .padding(top = Spacing.regular)
                        .fillMaxWidth(),
                    feedUrl = feedUrl,
                    showError = showError,
                    errorMessage = errorMessage,
                    onFeedUrlUpdated = onFeedUrlUpdated,
                )
            }
        }

        item {
            LinkOpeningPreferenceSelector(
                modifier = Modifier
                    .padding(top = Spacing.regular)
                    .fillMaxWidth(),
                currentPreference = feedSourceSettings.linkOpeningPreference,
                onPreferenceSelected = onLinkOpeningPreferenceSelected,
            )
        }

        item {
            Row(
                modifier = Modifier
                    .padding(top = Spacing.regular),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.hideFeedFromTimelineDescription,
                    modifier = Modifier.padding(top = Spacing.xsmall)
                        .weight(1f),
                )
                Switch(
                    checked = feedSourceSettings.isHiddenFromTimeline,
                    onCheckedChange = onHiddenToggled,
                    modifier = Modifier.padding(start = Spacing.regular),
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(top = Spacing.regular),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = LocalFeedFlowStrings.current.pinFeedSourceDescription,
                    modifier = Modifier.padding(top = Spacing.xsmall)
                        .weight(1f),
                )
                Switch(
                    checked = feedSourceSettings.isPinned,
                    onCheckedChange = onPinnedToggled,
                    modifier = Modifier.padding(start = Spacing.regular),
                )
            }
        }

        item {
            if (showNotificationToggle) {
                Row(
                    modifier = Modifier
                        .padding(top = Spacing.regular),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = LocalFeedFlowStrings.current.enableNotificationsForFeed,
                        modifier = Modifier.padding(top = Spacing.xsmall)
                            .weight(1f),
                    )
                    Switch(
                        checked = feedSourceSettings.isNotificationEnabled,
                        onCheckedChange = onNotificationToggleChanged,
                        modifier = Modifier.padding(start = Spacing.regular),
                    )
                }
            }
        }

        item {
            CategoriesSelector(
                modifier = Modifier
                    .padding(vertical = Spacing.regular),
                categoriesState = categoriesState,
                onClick = onCategorySelectorClick,
            )
        }

        item {
            Button(
                modifier = Modifier
                    .padding(top = Spacing.small)
                    .padding(bottom = Spacing.regular)
                    .fillMaxWidth(),
                enabled = feedUrl.isNotBlank() && !showLoading,
                onClick = editFeed,
            ) {
                if (showLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                } else {
                    Text(LocalFeedFlowStrings.current.actionSave)
                }
            }
        }

        item {
            TextButton(
                modifier = Modifier
                    .padding(bottom = Spacing.regular)
                    .fillMaxWidth(),
                onClick = onShowDeleteDialog,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(color = 0xFFD32F2F),
                ),
            ) {
                Text(LocalFeedFlowStrings.current.deleteFeedButton)
            }
        }

        item {
            Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
        }
    }
}
