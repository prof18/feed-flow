package com.prof18.feedflow.shared.ui.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.prof18.feedflow.core.model.FeedOperation
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.LocalFeedFlowStrings

@Composable
fun LoadingOperationDialog(
    feedOperation: FeedOperation,
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(Spacing.regular),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
                Text(
                    modifier = Modifier
                        .padding(start = Spacing.regular),
                    text = when (feedOperation) {
                        FeedOperation.Deleting -> LocalFeedFlowStrings.current.deletingFeedDialogTitle
                        FeedOperation.MarkingAllRead -> LocalFeedFlowStrings.current.markingAllReadDialogTitle
                        FeedOperation.None -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
