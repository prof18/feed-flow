package com.prof18.feedflow.settings.components

import FeedFlowTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.ui.preview.FeedFlowPreview
import com.prof18.feedflow.ui.theme.Spacing

@Composable
fun SettingsMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {

        Text(
            modifier = Modifier
                .padding(Spacing.regular),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@FeedFlowPreview
@Composable
private fun SettingsMenuItemPreview() {
    FeedFlowTheme {
        Surface {
            SettingsMenuItem(
                text = "Contact us",
                onClick = {

                }
            )
        }
    }
}
