package com.prof18.feedflow.android.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.prof18.feedflow.shared.ui.feedsuggestions.FeedSuggestionsE2eIds
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings
import md_theme_dark_background
import md_theme_light_background

@Composable
internal fun AddFeedOptionsBottomSheet(
    onAddFeedClick: () -> Unit,
    onFeedSuggestionsClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val strings = LocalFeedFlowStrings.current
    val containerColor = if (isSystemInDarkTheme()) {
        md_theme_dark_background
    } else {
        md_theme_light_background
    }

    ModalBottomSheet(
        containerColor = containerColor,
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ListItem(
            modifier = androidx.compose.ui.Modifier.clickable {
                onAddFeedClick()
                onDismiss()
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(strings.addFeed) },
            colors = ListItemDefaults.colors(containerColor = containerColor),
        )

        ListItem(
            modifier = Modifier
                .testTag(FeedSuggestionsE2eIds.ADD_OPTIONS_ITEM)
                .clickable {
                    onFeedSuggestionsClick()
                    onDismiss()
                },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(strings.feedSuggestionsTitle) },
            colors = ListItemDefaults.colors(containerColor = containerColor),
        )

        ListItem(
            modifier = androidx.compose.ui.Modifier.clickable {
                onImportExportClick()
                onDismiss()
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                )
            },
            headlineContent = { Text(strings.importFeedButton) },
            colors = ListItemDefaults.colors(containerColor = containerColor),
        )
    }
}
