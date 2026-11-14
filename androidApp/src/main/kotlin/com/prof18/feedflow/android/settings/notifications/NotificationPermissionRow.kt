package com.prof18.feedflow.android.settings.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.android.ui.preview.PreviewPhone
import com.prof18.feedflow.android.ui.style.Spacing
import com.prof18.feedflow.android.ui.theme.FeedFlowTheme
import com.prof18.feedflow.android.ui.utils.LocalFeedFlowStrings

/** Shown only while FeedFlow can't post notifications; styled like the other rows on the page. */
@Composable
internal fun NotificationPermissionRow(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalFeedFlowStrings.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular, vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.settingsNotificationsPermissionTitle,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = strings.settingsNotificationsPermissionOff,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FilledTonalButton(onClick = onRequestPermission) {
            Text(strings.settingsNotificationsPermissionAllow)
        }
    }
}

@PreviewPhone
@Composable
private fun NotificationPermissionRowPreview() {
    FeedFlowTheme {
        NotificationPermissionRow(onRequestPermission = {})
    }
}
