package com.prof18.feedflow.home.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.MR
import com.prof18.feedflow.ui.style.Spacing
import dev.icerock.moko.resources.compose.stringResource
import md_theme_dark_background
import md_theme_light_background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NoFeedsBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit
) {
    ModalBottomSheet(
        containerColor = if (isSystemInDarkTheme()) {
            md_theme_dark_background
        } else {
            md_theme_light_background
        },
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular),
                text = stringResource(MR.strings.no_feed_modal_title),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                modifier = Modifier
                    .padding(vertical = Spacing.regular)
                    .padding(horizontal = Spacing.regular),
                text = stringResource(MR.strings.no_feed_modal_message),
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.regular),
                onClick = {
                    onDismissRequest()
                    onAddFeedClick()
                },
            ) {
                Text(
                    stringResource(MR.strings.add_feed),
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.regular)
                    .padding(bottom = Spacing.medium)
                    .padding(horizontal = Spacing.regular),
                onClick = {
                    onDismissRequest()
                    onImportExportClick()
                },
            ) {
                Text(
                    stringResource(MR.strings.import_export_opml),
                )
            }
        }
    }
}