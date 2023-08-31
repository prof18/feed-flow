package com.prof18.feedflow.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.prof18.feedflow.ui.style.Spacing

@Composable
fun SettingsMenuItem(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Text(
            modifier = Modifier
                .padding(Spacing.regular),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
