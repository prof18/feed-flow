package com.prof18.feedflow.android.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.prof18.feedflow.android.ui.style.Spacing

@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = SettingRowDefaults.sectionHeaderStyle,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.regular,
                end = Spacing.regular,
                top = Spacing.medium,
                bottom = Spacing.small,
            )
            .semantics { heading() },
    )
}
