package com.prof18.feedflow.desktop.home.drawer

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prof18.feedflow.shared.ui.style.Spacing

@Composable
internal fun DrawerDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier
            .padding(vertical = Spacing.regular),
        thickness = 0.2.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
