package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.MR
import com.prof18.feedflow.core.utils.TestingTag
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.tagForTesting
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun NoFeedsInfoContent(
    showTitle: Boolean = true,
    onDismissRequest: () -> Unit,
    onAddFeedClick: () -> Unit,
    onImportExportClick: () -> Unit,
) {
    Column {
        if (showTitle) {
            Text(
                modifier = Modifier
                    .padding(horizontal = Spacing.regular),
                text = stringResource(MR.strings.no_feed_modal_title),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            modifier = Modifier
                .padding(vertical = Spacing.regular)
                .padding(horizontal = Spacing.regular)
                .tagForTesting(TestingTag.NO_FEED_BOTTOM_SHEET_MESSAGE),
            text = stringResource(MR.strings.no_feed_modal_message),
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.regular)
                .tagForTesting(TestingTag.NO_FEED_BOTTOM_SHEET_ADD_BUTTON),
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
                .padding(horizontal = Spacing.regular)
                .tagForTesting(TestingTag.NO_FEED_BOTTOM_SHEET_IMPORT_BUTTON),
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
