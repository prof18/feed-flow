package com.prof18.feedflow.shared.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.prof18.feedflow.shared.ui.style.Spacing
import com.prof18.feedflow.shared.ui.utils.PreviewTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SettingSelectorItem(
    title: String,
    currentValueLabel: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(vertical = Spacing.small)
            .padding(horizontal = Spacing.regular),
        horizontalArrangement = Arrangement.spacedBy(Spacing.regular),
    ) {
        Icon(
            icon,
            contentDescription = null,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = currentValueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun SettingSelectorItemPreview() {
    PreviewTheme {
        SettingSelectorItem(
            title = "Feed Order",
            currentValueLabel = "Newest First",
            icon = Icons.AutoMirrored.Outlined.Sort,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun SettingSelectorItemLongTextPreview() {
    PreviewTheme {
        SettingSelectorItem(
            title = "Auto Delete Period",
            currentValueLabel = "After 2 weeks of reading",
            icon = Icons.Outlined.DeleteSweep,
            onClick = {},
        )
    }
}
