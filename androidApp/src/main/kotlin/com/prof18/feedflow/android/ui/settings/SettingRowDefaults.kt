package com.prof18.feedflow.android.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared typography and spacing for settings rows, so every row type reads the same: a 16sp
 * regular title over a 14sp description, 4dp apart, with 12dp of vertical padding.
 */
object SettingRowDefaults {
    val verticalPadding = 12.dp
    val titleDescriptionSpacing = 4.dp

    val titleStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)

    val descriptionStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, letterSpacing = 0.sp)

    val descriptionColor: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val sectionHeaderStyle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 21.sp)
}
