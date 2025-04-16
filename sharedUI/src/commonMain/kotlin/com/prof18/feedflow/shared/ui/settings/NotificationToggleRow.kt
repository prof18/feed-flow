package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.prof18.feedflow.shared.ui.style.Spacing

@Composable
fun NotificationToggleRow(
    modifier: Modifier = Modifier,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.regular, vertical = Spacing.xsmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = {
                onCheckedChange(it)
            },
            modifier = Modifier.padding(start = Spacing.regular),
        )
    }
}
